import setuptools
import os

with open("%s/../README.md" % os.environ['PWD'], "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="antlr-denter",
    author="yshavit, Bluepuff",
    version="1.3.1",
    description="Python-like indentation tokens for ANTLR4",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/yshavit/antlr-denter",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
	install_requires=['antlr4-python3-runtime'],
    python_requires='>=3',
)
