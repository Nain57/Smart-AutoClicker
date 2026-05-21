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
# NCNN conversion via PNNX
# ----------------------------

def convert_pnnx(onnx_path, output_dir, shape=None, fp16=True):
    """
    Converts an ONNX model to NCNN format using PNNX.

    :param onnx_path: Path to the input ONNX model.
    :param output_dir: Directory where the NCNN model files will be saved.
    :param shape: Optional input shape to freeze (e.g., "1,3,48,320").
    :param fp16: Whether to enable FP16 mode.
    """
    print("\n=== Converting to NCNN (PNNX) ===")

    os.makedirs(output_dir, exist_ok=True)

    # pnnx works on filename inside directory context
    cwd = os.getcwd()

    try:
        os.chdir(output_dir)

        cmd = ["pnnx", onnx_path]

        if shape:
            cmd.append(f"input_shape=[{shape}]")

        if fp16:
            cmd.append("fp16=1")

        run(cmd)

    finally:
        os.chdir(cwd)


# ----------------------------
# MAIN
# ----------------------------

def main():
    """
    Main entry point for the ONNX to NCNN converter script.
    """
    parser = argparse.ArgumentParser(description="Convert an ONNX model to NCNN using PNNX.")

    parser.add_argument("--input", required=True, help="Input ONNX file")
    parser.add_argument("--output", required=True, help="Output folder for NCNN model")
    parser.add_argument("--shape", default="", help="Input shape e.g. 1,3,48,320")
    parser.add_argument("--fp16", action="store_true", help="Enable FP16 mode")

    args = parser.parse_args()

    onnx_path = os.path.abspath(args.input)
    output_dir = os.path.abspath(args.output)

    print("\n=== ONNX → NCNN Conversion ===")
    print("[INFO] Input:", onnx_path)
    print("[INFO] Output Folder:", output_dir)

    convert_pnnx(
        onnx_path,
        output_dir,
        args.shape if args.shape else None,
        args.fp16
    )

    print("\n[DONE] NCNN model generated successfully")


if __name__ == "__main__":
    main()
