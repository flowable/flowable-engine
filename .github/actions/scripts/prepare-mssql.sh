#!/bin/sh

sudo su
curl https://packages.microsoft.com/keys/microsoft.asc | sudo apt-key add -
echo "Installing mssql-tools"
sudo apt-get update
sudo apt-get install mssql-tools18 unixodbc-dev

echo "Creating database and user"
/opt/mssql-tools18/bin/sqlcmd -S ${MSSQL_HOST},${MSSQL_PORT} -U sa -P flowableStr0ngPassword -C -l 120 -i ./.github/actions/scripts/init-mssql.sql

if [ $? -ne 0 ]
then
  echo "Could not create database and user"
  exit $?
fi
echo "Created database and user"
