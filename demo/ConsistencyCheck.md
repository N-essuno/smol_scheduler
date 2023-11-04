# Check for consistency

We obtain the pot information with the asset model

```SPARQL
PREFIX ast: <http://www.semanticweb.org/gianl/ontologies/2023/1/sirius-greenhouse#>
SELECT ?shelfFloor ?groupPosition ?potPosition
WHERE {
    ?pot rdf:type ast:Pot ;
        ast:hasShelfFloor ?shelfFloor ;
        ast:hasGroupPosition ?groupPosition ;
        ast:hasPotPosition ?potPosition .
}
```

This will return in SMOL a `List<Pot>`. With this List we can query the influx db to check whether there are some data for which the pot is not present in the greenhouse.

```SMOL
from(bucket: "greenhouse")
    |> range(start: -30d)
    |> filter(fn: (r) => r.["_measurement"] == "ast:pot")
    |> filter(fn: (r) => r["shelf_floor"] != %1 or
        r["group_position"] != %2 or
        r["pot_position"] != %3)
    |> group(columns: ["shelf_floor", "group_position", "pot_position"])
```

This should find some discrepancies with the data.
