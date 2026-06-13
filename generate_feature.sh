#!/bin/bash

rootFolder="`pwd`"

kotlin "$rootFolder/.templates/hierarchy_generator.kts" --env "$rootFolder/.templates/standard_module_kts/.env" -o "$rootFolder/features" -ex "kt,gradle,md" "$rootFolder/.templates/standard_module_kts/{{\$module_path}}"
