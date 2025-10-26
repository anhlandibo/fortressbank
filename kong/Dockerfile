# Kong Gateway 3.3.0 with openid-connect plugin (renamed from kong-openid-connect)
FROM kong:3.3.0 AS builder

USER root

# Upgrade LuaRocks
ENV LUAROCKS_VERSION=3.12.1
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        build-essential \
        git \
        curl && \
    curl -fsSL -o luarocks.tar.gz https://luarocks.org/releases/luarocks-${LUAROCKS_VERSION}.tar.gz && \
    tar zxpf luarocks.tar.gz && \
    cd luarocks-${LUAROCKS_VERSION} && \
    ./configure --with-lua=/usr/local/openresty/luajit --with-lua-include=/usr/local/openresty/luajit/include/luajit-2.1 && \
    make && make install && \
    cd .. && rm -rf luarocks-${LUAROCKS_VERSION}* luarocks.tar.gz

# Clone the plugin and rename it to openid-connect
RUN git clone https://github.com/cuongntr/kong-openid-connect-plugin.git /tmp/kong-plugin && \
    mv /tmp/kong-plugin/kong/plugins/kong-openid-connect /tmp/openid-connect && \
    # Fix all internal references from kong-openid-connect to openid-connect
    find /tmp/openid-connect -type f -name "*.lua" -exec sed -i 's/kong\.plugins\.kong-openid-connect/kong.plugins.openid-connect/g' {} \;

# --- Final Stage ---
FROM kong:3.3.0

USER root

# Install dependencies
ENV LUAROCKS_VERSION=3.12.1
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        build-essential \
        curl && \
    curl -fsSL -o luarocks.tar.gz https://luarocks.org/releases/luarocks-${LUAROCKS_VERSION}.tar.gz && \
    tar zxpf luarocks.tar.gz && \
    cd luarocks-${LUAROCKS_VERSION} && \
    ./configure --with-lua=/usr/local/openresty/luajit --with-lua-include=/usr/local/openresty/luajit/include/luajit-2.1 && \
    make && make install && \
    cd .. && rm -rf luarocks-${LUAROCKS_VERSION}* luarocks.tar.gz && \
    luarocks install lua-resty-openidc && \
    luarocks install lua-resty-session && \
    luarocks install lua-resty-http && \
    apt-get purge -y --auto-remove build-essential curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy the renamed plugin to the right location
COPY --from=builder /tmp/openid-connect /usr/local/share/lua/5.1/kong/plugins/openid-connect

USER kong

# Enable the plugin with:
# KONG_PLUGINS=bundled,openid-connect