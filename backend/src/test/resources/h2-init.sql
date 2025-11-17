CREATE ALIAS IF NOT EXISTS JSON_VALUE AS $$
String jsonValue(String json, String path) throws Exception {
    if (json == null || path == null) {
        return null;
    }

    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);

    String fieldName = path.replace("$.", "").replace("'", "");

    com.fasterxml.jackson.databind.JsonNode node = root.get(fieldName);
    return node != null ? node.asText() : null;
}
$$;
