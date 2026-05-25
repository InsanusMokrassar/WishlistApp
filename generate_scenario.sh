#!/bin/bash

rootFolder="`pwd`"

kotlin "$rootFolder/.templates/hierarchy_generator.kts" --env "$rootFolder/.templates/client_module_kts/.env" -o "$rootFolder/features/ui" -ex "kt,gradle,md" "$rootFolder/.templates/client_module_kts/{{\$module_path}}"
