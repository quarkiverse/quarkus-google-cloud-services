#!/bin/sh

user_exists(){ id "$1" &>/dev/null; }
group_exists(){ getent group "$1" &>/dev/null; }
group_name(){ getent group "$1" | cut -d ':' -f 1; }

user="$1"
group="$2"

echo "Running as user $user : $group , checking if they need to be added..."

if group_exists "$group" ; then
  echo "Group already exists, reusing existing group"
else
  addgroup -g "$group" runner
fi

groupName=$(group_name "$group")

echo "Running as group name $groupName"

if user_exists "$user" ; then
  echo "User already exists, skipping adding"
else
  adduser -u "$user" -G "$groupName" -D -h /srv/firebase runner
fi
