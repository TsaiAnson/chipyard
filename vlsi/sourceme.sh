#!/bin/sh

# Uncomment this if you want to run setup.py for other servers, but this should work fine for inst servers.
#if [ ! -f env_json_path.txt ]; then
#    >&2 echo "You must run ./setup.py to create env_json_path.txt for the path to the .json file with your institutional settings. You can also create the file yourself - e.g. /path/to/.../cad.json"
#else
#source ~ee241/tutorials/ee241.bashrc
export HAMMER_HOME="$PWD/hammer"
# We're only going to use Cadence tools for this class
export HAMMER_ENVIRONMENT_CONFIGS="$PWD/inst-env.yml"
export HAMMER_DRIVER_OBJ_DIR="$PWD/obj"
export PATH="/share/instsww/cadence/INNOVUS171/bin:$PATH"
source $HAMMER_HOME/sourceme.sh
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
export MGC_HOME="/share/instsww/mgc/calibre2017/aoi_cal_2017.4_35.25"
export CALIBRE_HOME="/share/instsww/mgc/calibre2017/aoi_cal_2017.4_35.25"
export PATH="$PATH:$CALIBRE_HOME/bin"
export PDK_DIR="/home/ff/ee241/spring20-labs/asap7PDK_r1p5"
export MGLS_LICENSE_FILE="1717@license-srv.eecs.berkeley.edu"

source /home/ff/ee290-2/chipyard-env.sh
# fi
