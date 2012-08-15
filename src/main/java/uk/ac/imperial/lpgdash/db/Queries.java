package uk.ac.imperial.lpgdash.db;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Queries {

	public static String getQuery(String name) {
		InputStream is = Queries.class.getResourceAsStream("/sql/" + name
				+ ".sql");
		try {
			return new Scanner(is).useDelimiter("\\A").next();
		} catch (NoSuchElementException e) {
			return "";
		} catch (NullPointerException e) {
			return "";
		}
	}

}
