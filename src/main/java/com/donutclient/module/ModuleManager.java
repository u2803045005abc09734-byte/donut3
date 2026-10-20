package com.donutclient.module;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
	private static final List<ClientModule> MODULES = new ArrayList<>();

	private ModuleManager() {}

	public static void init() {
		if (!MODULES.isEmpty()) {
			return;
		}
		MODULES.add(new StorageFinder());
		MODULES.add(new SpawnerFinder());
	}

	public static List<ClientModule> all() {
		return MODULES;
	}

	public static List<FinderModule> finders() {
		List<FinderModule> out = new ArrayList<>();
		for (ClientModule m : MODULES) {
			if (m instanceof FinderModule f) {
				out.add(f);
			}
		}
		return out;
	}

	public static int activeCount() {
		int n = 0;
		for (ClientModule m : MODULES) {
			if (m.isEnabled()) {
				n++;
			}
		}
		return n;
	}
}
