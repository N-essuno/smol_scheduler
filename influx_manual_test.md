# Manual testing for InfluxDB

Query to get last moisture measurement about the pot in the right group, left position, first floor

``` SQL
from(bucket: "greenhouse_test")
  |> range(start: -30d)
  |> filter(fn: (r) => r["_measurement"] == "ast:pot")
  |> filter(fn: (r) => r["_field"] == "moisture")
  |> filter(fn: (r) => r["group_position"] == "right")
  |> filter(fn: (r) => r["pot_position"] == "left")
  |> filter(fn: (r) => r["shelf_floor"] == "1")
  |> keep(columns: ["_value"])
  |> last()
```

Asset test data in line protocol format to load into InfluxDB for testing

The timestamp is set to (DD/MM/YYYY): `19/05/2023 10:00:00 GMT+0200 (Central European Summer Time)`

> Note: the timestamp precision used here is seconds. When uploading from influxDB dashboard you need to change the precision to seconds

```SQL
ast:pot,shelf_floor=1,group_position=left,pot_position=left,plant_id=1 moisture=20.0 1684483200
ast:pot,shelf_floor=1,group_position=left,pot_position=right,plant_id=2 moisture=60.0 1684483200

ast:shelf,shelf_floor=1 humidity=15.0,temperature=23.0 1684483200
ast:shelf,shelf_floor=2 humidity=10.0,temperature=19.0 1684483200

ast:greenhouse light=80.0 1684483200

ast:pump,shelf_floor=1,group_position=left pumped_water=15.0 1684483200
ast:pump,shelf_floor=2,group_position=left pumped_water=15.0 1684483200

ast:plant,plant_id=1 ndvi=-0.4 1684483200
ast:plant,plant_id=2 ndvi=0.2 1684483200
```
