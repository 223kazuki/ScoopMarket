#!/bin/bash

lein do clean, build
heroku git:remote -a scoopmarket
git checkout -B deploy
git add -f resources/public
git commit -m "deploy"
git push --force heroku deploy:master
git checkout master
