#! /bin/sh

if [ "$#" -lt 7 ]; then
  echo "Please enter all arguments followed by symbols"
  exit 1
fi

key="$1"
hostname="$2"
port="$3"
database="$4"
user="$5"
password="$6"
shift 6

query_api() {
  local symbol="$1"
  curl --request GET \
    --url "https://alpha-vantage.p.rapidapi.com/query?function=GLOBAL_QUOTE&symbol=$symbol&datatype=json" \
    --header "X-RapidAPI-Host: alpha-vantage.p.rapidapi.com" \
    --header "X-RapidAPI-Key: $key"
}

add_row() {
  local symbol=$1
  local res=$2
  
  open=$("$res" | jq -r '."Global Quote"."02. open"')
  high=$("$res" | jq -r '."Global Quote"."03. high"')
  low=$("$res" | jq -r '."Global Quote"."04. low"')
  price=$("$res" | jq -r '."Global Quote"."05. price"')
  volume=$("$res" | jq -r '."Global Quote"."06. volume"')
  
  psql -h "$hostname" -p "$port" -d "$database" -U "$user" -W "$password" \
    -c "INSERT INTO quotes (symbol, open, high, low, price, volume) VALUES ('$symbol', $open, $high, $low, $price, $volume);"
}

for symbol in "$@"; do
  res=$(query_api "$symbol")  

  if [[ -z $("$res" | jq -r '.["Global Quote"]') ]]; then
    echo "Invalid or mising response. Please check to make sure $symbol is the correct symbol"
    exit 1
  fi  
  
  add_row "$symbol" "$res"
done
