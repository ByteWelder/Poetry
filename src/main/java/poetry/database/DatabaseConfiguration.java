package poetry.database;

public class DatabaseConfiguration {
	public static final String DEFAULT_NAME = "database";
	private final int modelVersion;
	private final Class<?>[] modelClasses;
	private final String databaseName;

	public DatabaseConfiguration(int modelVersion, Class<?>[] modelClasses, String databaseName) {
		this.modelVersion = modelVersion;
		this.modelClasses = modelClasses;
		this.databaseName = databaseName;
	}

	public DatabaseConfiguration(int modelVersion, Class<?>[] modelClasses) {
		this(modelVersion, modelClasses, DEFAULT_NAME);
	}


	public int getModelVersion() {
		return modelVersion;
	}

	public Class<?>[] getModelClasses() {
		return modelClasses;
	}

	public String getDatabaseName() {
		return databaseName;
	}
}

