Poetry
======

[![License][license-svg]][license-link]

Poetry is an Android persistence library that allows you to persist a JSON object tree directly into an SQLite database through [OrmLite].
Poetry enables you to write less code and persist data much faster.

Poetry is forked from [github.com/elastique/poetry](https://github.com/elastique/poetry) and is now maintained here, by the original developer.

Consider this JSON:
```json
{
	"id" : 1,
	"name" : "John Doe"
}
```
And this Java model:
```kotlin
@DatabaseTable
class Model(
	@DatabaseField(id = true)
	var id: Long,

	@DatabaseField
	var value: String
) {
	internal constructor(): this(0, "")
}
```

They can be stored into the database like this:

```kotlin
val jsonObject = .. // processed JSON tree
val databaseHelper = .. // OrmLite databasehelper;
val poetry = Poetry(databaseHelper.writableDatabase)
poetry.writeObject(User.class, jsonObject)
```

## Features ##

* Annotation-based model configuration
* Advanced `DatabaseHelper` with easy-to-use `DatabaseConfiguration`
* Support for relationships:
	* One-to-one
	* Many-to-one
	* One-to-many
	* Many-to-many
* Persist objects and arrays of objects
* Persist objects with arrays of base types (e.g. JSON String array persisted to separate table)

## Requirements ##

- Android 5.0 (API level 21) or higher

## Usage ##

**build.gradle**

```groovy
repositories {
    jcenter()
}
```

```groovy
dependencies {
    compile (
        [group: 'com.bytewelder.poetry', name: 'poetry', version: '5.0.0']
    )
}
```

## Behaviors ##

 * Models that are imported more than once within a single JSON tree are updated every time they are processed.
 * Attributes that are not specified in JSON are not updated
 * Attributes that are imported with null value will have a null value in the database. Make sure your model allows this.
 * When you use the `poetry.writeArray()` method, it will import the array and delete all objects from the database that do not correspond to any of the imported id values.

## Tutorial ##

### Creating a DatabaseHelper ###

The Poetry `DatabaseHelper` allows you to easily configure your database.
In the example below, you can see a custom `DatabaseHelper` with a `DatabaseConfiguration` that holds the model version and model classes.

**MyDatabaseHelper.kt**
```kotlin
class BasicHelper(context: Context) : poetry.DatabaseHelper(
	context,
	databaseConfigurationOf(
		"DatabaseName",
        ModelA::class,
        ModelB::class
        ModelC::class
	)
)
```

### Mapping custom JSON properties ###

By default, the name of the attribute is used to map from JSON. Your naming conventions might now allow this. You can specify the json key name with the `@MapFrom` annotation.

**User.java**
```java
@DatabaseTable
class User
{
	@DatabaseField(id = true, columnName = "id")
	@MapFrom("id")
    private int mId;

	@DatabaseField(columnName = "name")
	@MapFrom("name")
    private String mName;
}
```

### One-to-many relationships ###

In this example, a `Game` object holds a list of `Player` objects. 

**game.json**
```json
{
    "id": 1,
    "players" : [
        {
	        "id": 1,
	        "name": "John"
        },
        {
	        "id": 2,
	        "name": "Jane"
        }
    ]
}
```
**Game.java**
```java
@DatabaseTable
public class Game
{
    @DatabaseField(id = true)
    public int id;
    
    @DatabaseField(foreign = true)
    public Player player;
}
```
**Player.java**
```java
@DatabaseTable
public class Player
{
    @DatabaseField(id = true)
    public int id;

    @DatabaseField
    public String name;

	@ForeignCollectionField(eager = true)
	public ForeignCollection<Player> players;
}
```

### Many-to-many relationships ###

In this example, a `User` can have 0 or more `Groups`, and a `Group` can have 0 or more `Users`.

**users.json**
```json
[
	{
	    "id" : 1,
	    "name" : "John",
	    "groups" : [
		    {
			    "id" : 1,
			    "name" : "Group 1"
		    },
		    {
			    "id" : 2,
			    "name" : "Group 2"
		    }
	    ]
	},
	{
	    "id" : 2,
	    "name" : "Jane",
	    "groups" : [
		    {
			    "id" : 1,
			    "name" : "Group 1"
		    }
	    ]
	}
]
```

**User.java**
```java
@DatabaseTable
public class User
{
    @DatabaseField(id = true)
    public int id;

    @DatabaseField
    public String name;
    
    /**
     * Many-to-many relationships.
     *
     * OrmLite requires a ForeignCollectionField with the helper-type UserGroup to assist in the database relational mapping.
     * JSON-to-SQLite persistence also requires the additional annotation "ManyToManyField"
     */
    @ForeignCollectionField(eager = true)
    @ManyToManyField(targetType = Group.class)
	public ForeignCollection<UserGroup> groups;
}
```
**UserGroup.java**
```java
@DatabaseTable
public class UserGroup
{
    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField(foreign = true)
	public User user;

    @DatabaseField(foreign = true)
    public Group group;
}
```

**Group.java**
```java
@DatabaseTable
public class Group
{
    @DatabaseField(id = true)
    public int id;

    @DatabaseField
    public String name;
}
```

### One-to-one relationships ###

In this example, a `User` can have 0 or 1 `Friend` user.

**users.json**
```json
[
	{
	    "id": 1,
	    "name" : "John",
	    "friend" : { "id" : 2 }
	},
	{
	    "id": 2,
	    "name" : "Jane",
	    "friend" : { "id" : 1 }
	}
]
```

The following alternative JSON is also valid:

**users.json**
```json
[
	{
	    "id": 1,
	    "name" : "John",
	    "friend" : 2
	},
	{
	    "id": 2,
	    "name" : "Jane",
	    "friend" : 1
	}
]
```

**User.java**
```java
@DatabaseTable
public class User
{
    @DatabaseField(id = true)
    public int id;

    @DatabaseField
    public String name;
    
    @DatabaseField(foreign = true)
    public User friend;
}
```

### Arrays of base types ###

Arrays of base types work the same as one-to-many relationships. The only difference is that you have to define a model that holds the base types and use the `@ForeignCollectionFieldSingleTarget` annotation to specify the database column name that holds the value.

**user.json**
```json
{
    "id": 1,
    "tags" : [
        "tag1",
        "tag2"
    ]
}
```
**User.java**
```java
@DatabaseTable
public class User
{
    @DatabaseField(id = true)
    public int id;
    
	// The targetField refers to the table's column name
	@ForeignCollectionField(eager = true)
	@ForeignCollectionFieldSingleTarget(targetField = "value")
	@MapFrom("tags")
	public ForeignCollection<UserTag> userTags;
}
```
**UserTag.java**
```java
@DatabaseTable
public class UserTag
{
    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField(foreign = true)
    public User user;

    @DatabaseField
	public String value;
}
```

[license-svg]: https://img.shields.io/badge/license-Apache%202.0-lightgrey.svg?style=flat
[license-link]: https://github.com/ByteWelder/Poetry/blob/master/LICENSE
[OrmLite]: http://ormlite.com
[JSON]: http://json.org/java/
