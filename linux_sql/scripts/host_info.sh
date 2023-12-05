psql_host=$1
psql_port=$2
db_name=$3
psql_user=$4
psql_password=$5

if [ "$#" -ne 5 ]; then
    echo "Illegal number of parameters"
    exit 1
fi

vmstat_mb=$(vmstat --unit M)
lscpu_out=`lscpu`
hostname=$(hostname -f)

cpu_number=$(echo "$lscpu_out"  | egrep "^CPU\(s\):" | awk '{print $2}' | xargs)
cpu_architecture=$(echo "$lscpu_out" | grep "^Architecture:" | awk '{print $2}' | xargs)
cpu_model=$(echo "$lscpu_out" | grep "^Model name:" | awk '{$1=""; $2=""; print $0}' | xargs)
cpu_mhz=$(echo "$lscpu_out" | grep "^CPU MHz:" | awk '{print $3}' | xargs)
l2_cache=$(echo "$lscpu_out" | grep "^L2 cache:" | awk '{print $3, $4}' | xargs)
total_mem= $(vmstat --unit M | tail -1 | awk '{print $4}')
timestamp=$(vmstat -t | awk '{print $18" "$19}' | xargs)

insert_stmt="INSERT INTO host_info(timestamp, cpu_number, cpu_architecture, cpu_model, cpu_mhz, l2_cache, total_mem) VALUES('$timestamp', '$cpu_number', '$cpu_architecture', '$cpu_model', '$cpu_mhz', '$l2_cache', '$total_mem')";

#set up env var for pql cmd
export PGPASSWORD=$psql_password
#Insert date into a database
psql -h $psql_host -p $psql_port -d $db_name -U $psql_user -c "$insert_stmt"
exit $?
