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

import os
import argparse
import subprocess
import sys

# ----------------------------
# Run command helper
# ----------------------------

def run(cmd):
    """
    Executes a shell command and captures its output.

    :param cmd: List of command arguments.
    """
    print("\n[CMD]", " ".join(cmd))

    result = subprocess.run(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="replace"
    )

    print(result.stdout)

    if result.returncode != 0:
        print("[ERROR]", result.stderr)
        sys.exit(1)


# ----------------------------
# ONNX simplification
# ----------------------------

def simplify_onnx(onnx_path, sim_path, shape=None):
    """
    Simplifies an ONNX model using onnxsim.

    :param onnx_path: Path to the input ONNX model.
    :param sim_path: Path where the simplified model will be saved.
    :param shape: Optional input shape to freeze (e.g., "1,3,48,320").
    """
    cmd = ["onnxsim", onnx_path, sim_path]

    if shape:
        cmd += ["--input-shape", shape]

    print("\n=== Simplifying ONNX ===")
    run(cmd)


# ----------------------------
# MAIN
# ----------------------------

def main():
    """
    Main entry point for the ONNX simplifier script.
    """
    parser = argparse.ArgumentParser(description="Simplify an ONNX model using onnxsim.")

    parser.add_argument("--input", required=True, help="Input ONNX file")
    parser.add_argument("--output", required=True, help="Output simplified ONNX file")
    parser.add_argument("--shape", default="", help="Input shape e.g. 1,3,48,320")

    args = parser.parse_args()

    onnx_path = os.path.abspath(args.input)
    sim_path = os.path.abspath(args.output)

    print("\n=== ONNX Simplification ===")
    print("[INFO] Input:", onnx_path)
    print("[INFO] Output:", sim_path)

    simplify_onnx(
        onnx_path,
        sim_path,
        args.shape if args.shape else None
    )

    print("\n[DONE] ONNX model simplified successfully")


if __name__ == "__main__":
    main()
