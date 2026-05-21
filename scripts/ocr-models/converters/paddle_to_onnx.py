# Copyright (C) 2026 Kevin Buzeau
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#   *
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

"""
This script routes model conversion requests to the paddle2onnx tool.
It automatically detects the model type (legacy .pdmodel or new .json format)
and runs the conversion with the appropriate parameters.
"""

import os
import argparse
import subprocess
import sys

# ----------------------------
# Detect model type
# ----------------------------

def detect_model_type(model_dir):
    """
    Analyzes the contents of the model directory to determine if it's a
    legacy format (.pdmodel) or a new format (.json).

    :param model_dir: Path to the directory containing the model files.
    :return: "new", "legacy", or None if the type cannot be determined.
    """
    files = os.listdir(model_dir)

    has_pdmodel = any(f.endswith(".pdmodel") for f in files)
    has_json = any(f.endswith(".json") for f in files)
    has_pdiparams = any(f.endswith(".pdiparams") for f in files)

    if has_json and has_pdiparams:
        return "new"

    if has_pdmodel and has_pdiparams:
        return "legacy"

    return None


# ----------------------------
# Find file helper
# ----------------------------

def find_file(model_dir, ext):
    """
    Searches for a file with the specified extension in the given directory.

    :param model_dir: Path to the directory to search.
    :param ext: The file extension to look for (e.g., ".pdmodel").
    :return: The name of the first file found with the extension, or None.
    """
    for f in os.listdir(model_dir):
        if f.endswith(ext):
            return f
    return None


# ----------------------------
# Run paddle2onnx
# ----------------------------

def run_paddle2onnx(model_dir, model_file, params_file, output_file):
    """
    Executes the paddle2onnx command-line tool.

    :param model_dir: Directory containing the model.
    :param model_file: The name of the model file (.pdmodel or .json).
    :param params_file: The name of the parameters file (.pdiparams).
    :param output_file: The path where the ONNX model should be saved.
    """
    cmd = [
        "paddle2onnx",
        "--model_dir", model_dir,
        "--model_filename", model_file,
        "--params_filename", params_file,
        "--save_file", output_file,
        "--opset_version", "11"
    ]

    print("\n[INFO] Running paddle2onnx:")
    print(" ".join(cmd))

    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

    print(result.stdout)

    if result.returncode != 0:
        print("[ERROR] paddle2onnx failed:")
        print(result.stderr)
        sys.exit(1)


# ----------------------------
# Main logic
# ----------------------------

def main():
    """
    Main entry point for the Paddle2ONNX router.
    Parses arguments, detects model type, and triggers the conversion.
    """
    parser = argparse.ArgumentParser()

    parser.add_argument("--input", required=True, help="Model directory")
    parser.add_argument("--output", required=True, help="Output ONNX file")

    args = parser.parse_args()

def main_logic(input_dir, output_file):
    model_dir = os.path.abspath(input_dir)

    print("\n=== Paddle2ONNX Router===\n")
    print("[INFO] Input:", model_dir)

    mtype = detect_model_type(model_dir)

    if not mtype:
        raise ValueError("Invalid model: missing .pdmodel or .json + .pdiparams")

    print("[INFO] Detected type:", mtype)

    pdiparams = find_file(model_dir, ".pdiparams")

    if not pdiparams:
        raise ValueError("Missing .pdiparams file")

    if mtype == "legacy":
        pdmodel = find_file(model_dir, ".pdmodel")

        if not pdmodel:
            raise ValueError("Missing .pdmodel file")

        run_paddle2onnx(
            model_dir,
            pdmodel,
            pdiparams,
            output_file
        )

    elif mtype == "new":
        json_file = find_file(model_dir, ".json")

        if not json_file:
            raise ValueError("Missing .json file")

        run_paddle2onnx(
            model_dir,
            json_file,
            pdiparams,
            output_file
        )

# ----------------------------
# Main logic
# ----------------------------

def main():
    """
    Main entry point for the Paddle2ONNX router.
    Parses arguments, detects model type, and triggers the conversion.
    """
    parser = argparse.ArgumentParser()

    parser.add_argument("--input", required=True, help="Model directory")
    parser.add_argument("--output", required=True, help="Output ONNX file")

    args = parser.parse_args()

    main_logic(args.input, args.output)

    print("\n[DONE] ONNX exported successfully")


if __name__ == "__main__":
    main()
