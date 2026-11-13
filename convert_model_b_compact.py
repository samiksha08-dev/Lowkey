
"""
LOWKEY - Compact Isolation Forest ONNX Converter

Converts the original Model B into one compact
ONNX TreeEnsembleRegressor graph.

The ONNX input is a 7-feature FLOAT32 vector
after applying the saved StandardScaler.

Original JOBLIB model is never modified.
"""

from pathlib import Path
import json

import joblib
import numpy as np
import onnx
import onnxruntime as ort

from onnx import TensorProto, helper, numpy_helper


# ==================================================
# 1. PATHS
# ==================================================

BASE = Path(__file__).resolve().parent

MODEL_DIR = BASE / "LOWKEY_PROCESSED" / "model"

JOBLIB_PATH = MODEL_DIR / "lowkey_model_b_magnitude.joblib"

CANDIDATE_PATH = MODEL_DIR / "lowkey_model_b_compact_candidate.onnx"

FINAL_PATH = MODEL_DIR / "lowkey_model_b_compact.onnx"

METADATA_PATH = MODEL_DIR / "lowkey_model_b_onnx_metadata.json"


# ==================================================
# 2. LOAD ORIGINAL MODEL B
# ==================================================

print("\n[1/7] Loading original Model B...")

artifact = joblib.load(JOBLIB_PATH)

model = artifact["model"]
scaler = artifact["scaler"]
features = artifact["feature_names"]

n_features = len(features)
n_trees = len(model.estimators_)

assert n_features == 7
assert n_trees == 300
assert model.max_samples_ == 256

print("Trees:", n_trees)
print("Features:", n_features)
print("Maximum samples:", model.max_samples_)


# ==================================================
# 3. PREPARE TREE ENSEMBLE ATTRIBUTES
# ==================================================

print("\n[2/7] Reading all 300 tree structures...")

nodes_treeids = []
nodes_nodeids = []
nodes_featureids = []
nodes_modes = []
nodes_values = []
nodes_truenodeids = []
nodes_falsenodeids = []
nodes_missing_value_tracks_true = []

target_treeids = []
target_nodeids = []
target_ids = []
target_weights = []

total_nodes = 0
total_leaves = 0

if not hasattr(model, "_decision_path_lengths"):
    raise RuntimeError("Fitted tree path lengths are unavailable.")

if not hasattr(model, "_average_path_length_per_tree"):
    raise RuntimeError("Fitted average path lengths are unavailable.")

for tree_id, (estimator, feature_map) in enumerate(
    zip(model.estimators_, model.estimators_features_)
):

    tree = estimator.tree_

    feature_map = np.asarray(feature_map, dtype=np.int64)

    # Preserve the actual feature mapping of each tree.
    if len(feature_map) != n_features:
        raise RuntimeError(
            f"Unexpected feature mapping in tree {tree_id}"
        )

    if not np.array_equal(
        np.sort(feature_map),
        np.arange(n_features)
    ):
        raise RuntimeError(
            f"Unexpected feature subset in tree {tree_id}"
        )

    path_depths = model._decision_path_lengths[tree_id]
    average_lengths = model._average_path_length_per_tree[tree_id]

    left_children = tree.children_left
    right_children = tree.children_right

    for node_id in range(tree.node_count):

        nodes_treeids.append(tree_id)
        nodes_nodeids.append(node_id)

        nodes_missing_value_tracks_true.append(0)

        left = int(left_children[node_id])
        right = int(right_children[node_id])

        # -----------------------------
        # Internal branching node
        # -----------------------------

        if left != -1:

            local_feature = int(tree.feature[node_id])
            global_feature = int(feature_map[local_feature])

            nodes_featureids.append(global_feature)

            nodes_modes.append("BRANCH_LEQ")

            nodes_values.append(
                float(tree.threshold[node_id])
            )

            # True = left, False = right
            nodes_truenodeids.append(left)
            nodes_falsenodeids.append(right)

        # -----------------------------
        # Leaf node
        # -----------------------------

        else:

            nodes_featureids.append(0)
            nodes_modes.append("LEAF")
            nodes_values.append(0.0)

            nodes_truenodeids.append(0)
            nodes_falsenodeids.append(0)

            # Match Scikit-learn's path contribution:
            # depth + average_path_length(leaf_samples) - 1

            leaf_contribution = (
                float(path_depths[node_id])
                + float(average_lengths[node_id])
                - 1.0
            )

            target_treeids.append(tree_id)
            target_nodeids.append(node_id)
            target_ids.append(0)

            target_weights.append(leaf_contribution)

            total_leaves += 1

    total_nodes += tree.node_count

print("Trees processed:", n_trees)
print("Total tree nodes:", total_nodes)
print("Total leaves:", total_leaves)


# ==================================================
# 4. CREATE ONE COMPACT ONNX TREE ENSEMBLE
# ==================================================

print("\n[3/7] Creating compact ONNX graph...")

# StandardScaler is applied before the ONNX model.
# The ONNX graph receives standardized FLOAT32 inputs.
# Convert input to DOUBLE internally to preserve the
# original tree split thresholds accurately.

input_info = helper.make_tensor_value_info(
    "scaled_features",
    TensorProto.FLOAT,
    [None, n_features]
)

output_info = helper.make_tensor_value_info(
    "score_samples",
    TensorProto.FLOAT,
    [None, 1]
)

nodes_values_tensor = numpy_helper.from_array(
    np.asarray(nodes_values, dtype=np.float64),
    name="tree_thresholds"
)

target_weights_tensor = numpy_helper.from_array(
    np.asarray(target_weights, dtype=np.float64),
    name="leaf_path_contributions"
)

# Average path length for max_samples = 256.
def average_path_length(n):

    if n <= 1:
        return 0.0

    if n == 2:
        return 1.0

    return (
        2.0 * (np.log(n - 1) + np.euler_gamma)
        - 2.0 * (n - 1) / n
    )


c_max = average_path_length(model.max_samples_)

initializers = [
    numpy_helper.from_array(
        np.asarray([c_max], dtype=np.float32),
        name="c_max"
    ),
    numpy_helper.from_array(
        np.asarray([2.0], dtype=np.float32),
        name="two"
    )
]

nodes = [

    helper.make_node(
        "Cast",
        ["scaled_features"],
        ["scaled_features_double"],
        to=TensorProto.DOUBLE
    ),

    # One operator represents the complete forest.
    helper.make_node(
        "TreeEnsembleRegressor",
        ["scaled_features_double"],
        ["average_path_length"],
        domain="ai.onnx.ml",
        n_targets=1,
        aggregate_function="AVERAGE",
        post_transform="NONE",

        nodes_treeids=nodes_treeids,
        nodes_nodeids=nodes_nodeids,
        nodes_featureids=nodes_featureids,
        nodes_modes=nodes_modes,
        nodes_values_as_tensor=nodes_values_tensor,

        nodes_truenodeids=nodes_truenodeids,
        nodes_falsenodeids=nodes_falsenodeids,
        nodes_missing_value_tracks_true=(
            nodes_missing_value_tracks_true
        ),

        target_treeids=target_treeids,
        target_nodeids=target_nodeids,
        target_ids=target_ids,
        target_weights_as_tensor=target_weights_tensor
    ),

    # score_samples = -2 ** (-average_depth / c(256))
    helper.make_node(
        "Div",
        ["average_path_length", "c_max"],
        ["normalized_depth"]
    ),

    helper.make_node(
        "Neg",
        ["normalized_depth"],
        ["negative_depth"]
    ),

    helper.make_node(
        "Pow",
        ["two", "negative_depth"],
        ["positive_score"]
    ),

    helper.make_node(
        "Neg",
        ["positive_score"],
        ["score_samples"]
    )
]

graph = helper.make_graph(
    nodes,
    "LOWKEY_Compact_IsolationForest",
    [input_info],
    [output_info],
    initializer=initializers
)

onnx_model = helper.make_model(
    graph,
    producer_name="LOWKEY",
    producer_version="1.0",
    opset_imports=[
        helper.make_opsetid("", 17),
        helper.make_opsetid("ai.onnx.ml", 3)
    ]
)

# Use an IR version supported by our ONNX Runtime.
onnx_model.ir_version = 13

onnx.checker.check_model(onnx_model)

if CANDIDATE_PATH.exists():
    raise FileExistsError(
        f"Remove or rename the previous candidate first: {CANDIDATE_PATH}"
    )

CANDIDATE_PATH.write_bytes(
    onnx_model.SerializeToString()
)

print("Compact ONNX graph created.")
print("Total tree nodes:", total_nodes)


# ==================================================
# 5. VERIFY PYTHON VS ONNX SCORES
# ==================================================

print("\n[4/7] Verifying ONNX inference...")

data_path = (
    BASE
    / "LOWKEY_PROCESSED"
    / "features"
    / "lowkey_ml_features.csv"
)

df = __import__("pandas").read_csv(data_path)

touch = (
    (df["tap_count"] > 0)
    | (df["scroll_count"] > 0)
)

valid = touch & (df["acc_sample_count"] >= 30)

data = df.loc[valid].copy()

X_raw = data[features].to_numpy(dtype=np.float64)

# Use the original fitted scaler.
# Convert the result to float32 because the
# original Isolation Forest expects FLOAT32 inputs.

X_scaled = scaler.transform(X_raw)

X_scaled = np.ascontiguousarray(
    X_scaled,
    dtype=np.float32
)

# Original Scikit-learn reference scores.
python_scores = model.score_samples(X_scaled)

# ONNX Runtime inference.
session = ort.InferenceSession(
    str(CANDIDATE_PATH),
    providers=["CPUExecutionProvider"]
)

input_name = session.get_inputs()[0].name
output_name = session.get_outputs()[0].name

onnx_scores = session.run(
    [output_name],
    {input_name: X_scaled}
)[0].reshape(-1)

if len(python_scores) != len(onnx_scores):
    raise RuntimeError("Python/ONNX output count mismatch.")

max_error = float(
    np.max(np.abs(python_scores - onnx_scores))
)

mean_error = float(
    np.mean(np.abs(python_scores - onnx_scores))
)

threshold = float(
    artifact["anomaly_threshold"]
)

python_flags = python_scores < threshold
onnx_flags = onnx_scores < threshold

flag_mismatches = int(
    np.count_nonzero(python_flags != onnx_flags)
)

print("\n===== PYTHON VS ONNX =====")

print("Observations:", len(X_scaled))
print("Python score range:", python_scores.min(), python_scores.max())
print("ONNX score range:", onnx_scores.min(), onnx_scores.max())

print("Maximum score error:", max_error)
print("Mean score error:", mean_error)

print("Threshold:", threshold)
print("Threshold classification mismatches:", flag_mismatches)


# ==================================================
# 6. PROMOTE ONLY IF PARITY PASSES
# ==================================================

print("\n[5/7] Checking conversion quality...")

if max_error > 1e-5:
    raise RuntimeError(
        "ONNX score parity failed. "
        "Original JOBLIB model remains unchanged."
    )

if flag_mismatches != 0:
    raise RuntimeError(
        "Threshold classification mismatch detected. "
        "Do not integrate this ONNX model yet."
    )

print("Score parity passed.")
print("Threshold classification parity passed.")


# ==================================================
# 7. SAVE VERIFIED MODEL AND METADATA
# ==================================================

print("\n[6/7] Saving verified ONNX model...")

if FINAL_PATH.exists():
    raise FileExistsError(
        f"Refusing to overwrite existing model: {FINAL_PATH}"
    )

CANDIDATE_PATH.replace(FINAL_PATH)

metadata = {
    "model_name": "LOWKEY Model B",
    "algorithm": "IsolationForest",
    "trees": n_trees,
    "features": features,

    "input_contract": (
        "Seven StandardScaler-transformed FLOAT32 features"
    ),

    "scaler_mean": scaler.mean_.tolist(),
    "scaler_scale": scaler.scale_.tolist(),

    "minimum_accelerometer_samples": 30,

    "anomaly_threshold": threshold,

    "score_rule": "score < threshold means anomalous",

    "maximum_python_onnx_score_error": max_error,

    "mean_python_onnx_score_error": mean_error,

    "threshold_mismatches": flag_mismatches
}

with open(
    METADATA_PATH,
    "w",
    encoding="utf-8"
) as file:
    json.dump(metadata, file, indent=4)


# ==================================================
# FINAL SUMMARY
# ==================================================

print("\n[7/7] CONVERSION SUCCESSFUL")

print("ONNX model:", FINAL_PATH)
print("Metadata:", METADATA_PATH)

print("\nOriginal JOBLIB model preserved.")
print("Original datasets unchanged.")
