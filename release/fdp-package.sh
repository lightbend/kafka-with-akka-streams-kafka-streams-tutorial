#!/usr/bin/env bash
set -e

SCRIPT=`basename ${BASH_SOURCE[0]}`
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -P )"
ROOT_DIR="${DIR}/.."

CONTENT_FILE=${DIR}/content.txt
CONTENT=$(cat $CONTENT_FILE)

function usage {
  cat<< EOF
  This script packages the sample applications into fdp-sample-apps-<version>.zip
  in the project root directory.
  Usage: $SCRIPT VERSION [-h | --help]

  VERSION       E.g., 0.3.0. Required
  -h | --help   This message.
EOF
}

while [ $# -ne 0 ]
do
  case $1 in
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      echo "$0: ERROR: Unrecognized argument $1"
      usage
      exit 1
      ;;
    *)
      VERSION=$1
      ;;
  esac
  shift
done

if [[ -z "$VERSION" ]]
then
  echo "$0: ERROR: The version argument is required."
  usage
  exit 1
fi

OUTPUT_FILE_ROOT=fdp-akka-kafka-streams-modelServer-${VERSION}
OUTPUT_FILE=${OUTPUT_FILE_ROOT}.zip

staging=$DIR/staging
rm -rf $staging
mkdir -p $staging

mkdir -p $staging/$OUTPUT_FILE_ROOT
for f in ${CONTENT}; do cp -r ${ROOT_DIR}/$f $staging/$OUTPUT_FILE_ROOT/$f; done
cd $staging
echo running: zip -r ${OUTPUT_FILE} ${OUTPUT_FILE_ROOT}
zip -r ${OUTPUT_FILE} ${OUTPUT_FILE_ROOT}

rm -rf ${OUTPUT_FILE_ROOT}
