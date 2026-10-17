package dev.konocraft.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.konocraft.KonoCraft;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialogue scripts live in /konocraft/dialogues/*.json inside the jar. A file is an object mapping
 * dialogue ids to arrays of lines. Line fields:
 * <ul>
 *   <li>s - speaker key, n - optional custom speaker name, t - text</li>
 *   <li>c - choices: [[text, label], [text, label, condition]]</li>
 *   <li>do - actions run when the line is shown</li>
 *   <li>id - label, goto - jump after the line, end - finish after the line</li>
 *   <li>auto - advance automatically after N ticks (cutscenes), if - show the line only when true</li>
 * </ul>
 */
public final class Dialogues {
	public static final String[] FILES = {"prologue", "chapter1", "chapter2", "chapter3", "chapter4", "chapter5", "ending", "npcs", "activities", "life", "town", "casino"};

	public record Choice(String text, String label, String cond) {
	}

	public record Line(String label, String speaker, String name, String text, List<Choice> choices,
					   List<String> actions, String gotoLabel, boolean end, int auto, String cond) {
		public boolean hasText() {
			return text != null && !text.isEmpty();
		}
	}

	public record Dialogue(String id, List<Line> lines, Map<String, Integer> labels) {
	}

	private static Map<String, Dialogue> dialogues;

	private Dialogues() {
	}

	public static synchronized Map<String, Dialogue> all() {
		if (dialogues == null) load();
		return dialogues;
	}

	public static Dialogue get(String id) {
		return all().get(id);
	}

	public static void load() {
		Map<String, Dialogue> map = new HashMap<>();
		for (String file : FILES) {
			String path = "/konocraft/dialogues/" + file + ".json";
			try (InputStream in = Dialogues.class.getResourceAsStream(path)) {
				if (in == null) {
					KonoCraft.LOGGER.error("Missing dialogue file {}", path);
					continue;
				}
				JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
				for (Map.Entry<String, JsonElement> e : root.entrySet()) {
					map.put(e.getKey(), parse(e.getKey(), e.getValue().getAsJsonArray()));
				}
			} catch (Exception ex) {
				KonoCraft.LOGGER.error("Failed to load dialogue file {}", path, ex);
			}
		}
		dialogues = map;
		KonoCraft.LOGGER.info("Loaded {} dialogues", map.size());
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) ? o.get(key).getAsString() : null;
	}

	private static Dialogue parse(String id, JsonArray arr) {
		List<Line> lines = new ArrayList<>();
		Map<String, Integer> labels = new HashMap<>();
		for (JsonElement el : arr) {
			JsonObject o = el.getAsJsonObject();
			List<Choice> choices = new ArrayList<>();
			if (o.has("c")) {
				for (JsonElement ce : o.getAsJsonArray("c")) {
					JsonArray ca = ce.getAsJsonArray();
					choices.add(new Choice(ca.get(0).getAsString(), ca.size() > 1 ? ca.get(1).getAsString() : null,
						ca.size() > 2 ? ca.get(2).getAsString() : null));
				}
			}
			List<String> actions = new ArrayList<>();
			if (o.has("do")) {
				JsonElement d = o.get("do");
				if (d.isJsonArray()) {
					for (JsonElement a : d.getAsJsonArray()) actions.add(a.getAsString());
				} else {
					actions.add(d.getAsString());
				}
			}
			String label = str(o, "id");
			if (label != null) labels.put(label, lines.size());
			lines.add(new Line(label, str(o, "s"), str(o, "n"), str(o, "t"), choices, actions, str(o, "goto"),
				o.has("end") && o.get("end").getAsBoolean(), o.has("auto") ? o.get("auto").getAsInt() : 0, str(o, "if")));
		}
		return new Dialogue(id, lines, labels);
	}
}
