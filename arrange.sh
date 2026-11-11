#!/bin/bash
# Moves the flat uploaded files into the folder layout Gradle expects.
set -e
J=src/main/java/com/example/bloodmod
R=src/main/resources
A=$R/assets/bloodmod
D=$R/data
mkdir -p $J $A/lang $A/textures/item $A/models/item $A/models/block $A/blockstates $D/bloodmod/recipe $D/minecraft/tags/fluid
mv *.java $J/
mv fabric.mod.json bloodmod.accesswidener $R/
mv lang_en_us.json $A/lang/en_us.json
mv blood_bucket.png $A/textures/item/blood_bucket.png
mv model_item_blood_bucket.json $A/models/item/blood_bucket.json
mv model_block_blood.json $A/models/block/blood.json
mv blockstate_blood.json $A/blockstates/blood.json
mv recipe_blood_bucket.json $D/bloodmod/recipe/blood_bucket.json
mv tag_water.json $D/minecraft/tags/fluid/water.json
