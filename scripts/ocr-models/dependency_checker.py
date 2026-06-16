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

import sys
import re
import importlib
import shutil
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REQ_PATH = os.path.join(SCRIPT_DIR, "requirements.txt")

def parse_requirements(file_path: str):
    """
    Extract package names from requirements.txt.
    Ignores versions, comments, and options.
    """
    packages = []

    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()

            # ignore comments / empty lines
            if not line or line.startswith("#"):
                continue

            # remove version constraints: package==1.2.3, >=, <=, etc.
            pkg = re.split(r"[<>=!~]", line)[0].strip()

            # skip editable installs / git installs
            if pkg.startswith("-e") or pkg.startswith("git+") or pkg.startswith("."):
                continue

            packages.append(pkg)

    return packages


# Some packages install under a different module name than their pip package name.
PACKAGE_TO_MODULE = {
    "paddlepaddle": "paddle",
    "pyyaml": "yaml",
    "pillow": "PIL",
    "scikit-learn": "sklearn",
    "opencv-python": "cv2",
    "opencv-python-headless": "cv2",
}

# Packages that are CLI tools only — verified via PATH rather than import.
CLI_ONLY_PACKAGES = {"paddle2onnx", "pnnx"}


def check_dependencies(requirements_path: str = REQ_PATH):
    """
    Verify all required packages are importable.
    """

    try:
        required = parse_requirements(requirements_path)
    except FileNotFoundError:
        print(f"[ERROR] requirements file not found: {requirements_path}")
        sys.exit(1)

    missing = []

    for pkg in required:
        pkg_lower = pkg.lower()

        if pkg_lower in CLI_ONLY_PACKAGES:
            if shutil.which(pkg_lower) is None:
                missing.append(pkg)
                print(f"[MISSING] {pkg}")
            continue

        module_name = PACKAGE_TO_MODULE.get(pkg_lower, pkg_lower.replace("-", "_"))

        try:
            importlib.import_module(module_name)
        except ImportError:
            missing.append(pkg)
            print(f"[MISSING] {pkg}")

    if missing:
        print("\n[ERROR] Missing dependencies detected:\n")
        for m in missing:
            print(" -", m)

        print("\nFix with:")
        print("  pip install -r requirements.txt\n")

        sys.exit(1)

if __name__ == "__main__":
    check_dependencies()