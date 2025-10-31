#!/usr/bin/env python3
"""
Kong Configuration Validator
Validates kong.yml against Kong 3.x declarative config schema
"""

import yaml
import sys
from typing import Dict, List, Any

def validate_kong_config(config_path: str) -> tuple[bool, List[str]]:
    """Validate Kong configuration file"""
    errors = []
    warnings = []
    
    try:
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)
    except yaml.YAMLError as e:
        return False, [f"YAML Parse Error: {e}"]
    except FileNotFoundError:
        return False, [f"File not found: {config_path}"]
    
    # Check format version
    if '_format_version' not in config:
        errors.append("Missing '_format_version' field")
    elif config['_format_version'] not in ['2.1', '3.0']:
        errors.append(f"Invalid format version: {config['_format_version']}")
    
    # Validate services
    if 'services' in config:
        for idx, service in enumerate(config['services']):
            service_name = service.get('name', f'service[{idx}]')
            
            # Required fields
            if 'name' not in service:
                errors.append(f"Service {idx}: Missing 'name' field")
            if 'url' not in service:
                errors.append(f"Service '{service_name}': Missing 'url' field")
            
            # Validate routes
            if 'routes' in service:
                for ridx, route in enumerate(service['routes']):
                    route_name = route.get('name', f'route[{ridx}]')
                    if 'paths' not in route:
                        errors.append(f"Service '{service_name}' Route '{route_name}': Missing 'paths'")
    else:
        warnings.append("No services defined")
    
    # Validate plugins
    if 'plugins' in config:
        plugin_names = {}
        for idx, plugin in enumerate(config['plugins']):
            if 'name' not in plugin:
                errors.append(f"Plugin {idx}: Missing 'name' field")
                continue
            
            plugin_name = plugin['name']
            service_name = plugin.get('service', 'global')
            route_name = plugin.get('route', None)
            
            # Track plugin instances
            key = f"{plugin_name}:{service_name}:{route_name}"
            if key in plugin_names:
                warnings.append(f"Duplicate plugin: {plugin_name} on service '{service_name}'")
            plugin_names[key] = True
            
            # Validate known plugins
            known_plugins = [
                'rate-limiting', 'openid-connect', 'cors', 'jwt', 
                'key-auth', 'oauth2', 'acl', 'request-transformer',
                'response-transformer', 'file-log', 'http-log'
            ]
            
            if plugin_name not in known_plugins:
                warnings.append(f"Unknown/custom plugin: {plugin_name}")
            
            # Validate config
            if 'config' not in plugin:
                errors.append(f"Plugin '{plugin_name}': Missing 'config' field")
            
            # Specific plugin validations
            if plugin_name == 'rate-limiting':
                config_obj = plugin.get('config', {})
                if 'minute' not in config_obj and 'second' not in config_obj and 'hour' not in config_obj:
                    errors.append(f"Plugin 'rate-limiting': Must specify at least one time limit (second/minute/hour)")
                
                policy = config_obj.get('policy', 'local')
                if policy not in ['local', 'cluster', 'redis']:
                    errors.append(f"Plugin 'rate-limiting': Invalid policy '{policy}'")
            
            if plugin_name == 'openid-connect':
                config_obj = plugin.get('config', {})
                required = ['client_id', 'client_secret', 'discovery']
                for field in required:
                    if field not in config_obj:
                        errors.append(f"Plugin 'openid-connect': Missing required field '{field}'")
    
    return len(errors) == 0, errors + warnings

if __name__ == '__main__':
    config_path = sys.argv[1] if len(sys.argv) > 1 else 'kong/kong.yml'
    
    print(f"🔍 Validating Kong configuration: {config_path}")
    print("=" * 60)
    
    success, messages = validate_kong_config(config_path)
    
    if success:
        print("✅ Configuration is VALID")
        if messages:
            print("\n⚠️  Warnings:")
            for msg in messages:
                print(f"  - {msg}")
    else:
        print("❌ Configuration has ERRORS:")
        for msg in messages:
            print(f"  - {msg}")
        sys.exit(1)
    
    print("\n" + "=" * 60)
    print("✅ parse successful")
