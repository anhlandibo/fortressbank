local typedefs = require "kong.db.schema.typedefs"

return {
  name = "single-device",
  fields = {
    { consumer = typedefs.no_consumer },
    { protocols = typedefs.protocols_http },
    { config = {
        type = "record",
        fields = {
          { keycloak_url = { type = "string", required = true }, },
          { realm = { type = "string", required = true }, },
          { admin_token = { type = "string", required = true }, },
        },
      },
    },
  },
}


