#!/bin/bash

RES=../app/src/main/res

convert ic_launcher.png -resize 48x48 $RES/drawable-mdpi/ic_launcher.png
convert ic_launcher.png -resize 72x72 $RES/drawable-hdpi/ic_launcher.png
convert ic_launcher.png -resize 96x96 $RES/drawable-xhdpi/ic_launcher.png
convert ic_launcher.png -resize 144x144 $RES/drawable-xxhdpi/ic_launcher.png
