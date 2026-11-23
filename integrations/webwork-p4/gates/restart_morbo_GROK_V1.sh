#!/bin/bash
# GROK_V1_1 P4: restart only OUR Morbo (pid file), port 3020.
P4=/workspace/mw-p4-20261123
[ -f $P4/logs/morbo.pid ] && { pkill -P $(cat $P4/logs/morbo.pid) 2>/dev/null; kill $(cat $P4/logs/morbo.pid) 2>/dev/null; }
sleep 2; bash $P4/ww/p4/start_p4_GROK_V1_1.sh webwork
