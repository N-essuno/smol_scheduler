# Greenhouse SMOL code

This code provides a representation for the different assets we expect in the greenhouse:
- Greenhouse 
- Pot
- Shelf
- Pump
- Plant

For each asset a class is defined:

### Greenhouse (no fields)
> There is only one `Greenhouse` so it is not identified by any field and it's not retrieved from the asset model. <br>

#### Methods
- `getLight()`
    - Runs and return the result of an influxDB query which gets the last **light value** for the `Greenhouse`

<br>

### Shelf

> In each `Shelf` there is one **group of plants**, which is composed by **two plants**. In particular we track the `Pot` in which the plants are instead of the plant itself.<br>
> Each `Shelf` is indentified by its "floor" (`shelfFloor`)

#### Methods

- `getHumidity()`
    - Runs and return the result of an influxDB query which gets the last **humidity value** for the `Shelf`
- `getTemperature()`
    - Runs and return the result of an influxDB query which gets the last **temperature value** registered for the `Shelf`


<br>

### Pot
> `Pot` is a container for a plant. <br>
> It is indentified by
> 1. The shelf in which it is located (`shelfFloor`)
> 2. The position of the pot group in the `Shelf` (`groupPosition`). Can be left or right. <br>
> 3. The position of the pot in the group (`potPosition`). Can be left or right. <br>
> It also contains the information about which `Plant` is contained in the pot.

#### Methods

- `getMoisture()`
    - Runs and return the result of an influxDB query which gets the last **moisture value** registered for the `Pot`

<br>

### Plant

> Represents a plant contained in a `Pot`. <br>
> It is identified by a `plantId`

#### Methods

- `getHealthState()`
    - Runs and return the result of an influxDB query which gets the last **NDVI value** registered for the `Plant`

### Pump

> `Pump` represents a pump used to water a group of pots. <br>
> For each group of pots there is a pump. So it is identified by:
> 1. Its shelf (`shelfFloor`)
> 2. The watered pot group position on the `Shelf` (`groupPosition`)

<br>


There is one class used as access point to the asset model:

### AssetModel

> This class is used to retrieve the individuals from the asset model (represented by an [OWL ontology](../README.md#greenhouse-asset-model)) and convert them into SMOL objects. <br>
> It contains one method per asset (Shelf, Pot, Plant, Pump) which returns a list of the corresponding SMOL objects. <br>