/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.resourcemerger.impl;

import java.util.List;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

/**
 * {@inheritDoc}
 */
public class MergedResource extends AbstractResource {

    /** The resource resolver. */
    private final ResourceResolver resolver;

    /** Full path of the resource. */
    private final String path;

    /** Resource type. */
    private final String resourceType;

    /** Resource meta data. */
    private final ResourceMetadata metadata = new ResourceMetadata();

    /** Cache value map. */
    private final ValueMap properties;

    /** Root path */
    private final String mergedRootPath;

    /**
     * Constructor
     *
     * @param resolver      Resource resolver
     * @param mergeRootPath   Merge root path
     * @param relativePath    Relative path
     * @param mappedResources List of physical mapped resources' paths
     */
    MergedResource(final ResourceResolver resolver,
                   final String mergeRootPath,
                   final String relativePath,
                   final List<Resource> mappedResources,
                   final List<ValueMap> valueMaps,
                   final String mergedRootPath) {
        this.resolver = resolver;
        this.path = (relativePath.length() == 0 ? mergeRootPath : mergeRootPath + "/" + relativePath);
        this.properties = new MergedValueMap(valueMaps);
        this.resourceType = this.properties.get(ResourceResolver.PROPERTY_RESOURCE_TYPE, (relativePath.length() == 0 ? "/" : relativePath));
        metadata.put(MergedResourceConstants.METADATA_FLAG, true);
        final String[] resourcePaths = new String[mappedResources.size()];
        int i = 0;
        for(final Resource rsrc : mappedResources) {
            resourcePaths[i] = rsrc.getPath();
            i++;
        }
        metadata.put(MergedResourceConstants.METADATA_RESOURCES, resourcePaths);
        this.mergedRootPath = mergedRootPath;
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() {
        return this.path;
    }

    /**
     * {@inheritDoc}
     */
    public String getResourceType() {
        return this.resourceType;
    }

    /**
     * {@inheritDoc}
     */
    public String getResourceSuperType() {
        // So far, there's no concept of resource super type for a merged resource
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceResolver getResourceResolver() {
        return resolver;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) this.properties;
        }
        if (type == ModifiableValueMap.class) {
            final String paths[] = (String[])this.metadata.get(MergedResourceConstants.METADATA_RESOURCES);
            final String[] searchPaths = resolver.getSearchPath();
            final String lastSearchPath = searchPaths[searchPaths.length-1];

            if ( paths.length == 1 && paths[0].startsWith(lastSearchPath) ) {
                final Resource copyResource = resolver.getResource(paths[0]);
                if ( searchPaths.length == 1 ) {
                    return (AdapterType)copyResource.adaptTo(ModifiableValueMap.class);
                }
                final String prefix = searchPaths[searchPaths.length-2];
                final String createPath = prefix + path.substring(this.mergedRootPath.length() + 1);
                try {
                    final Resource newResource = ResourceUtil.getOrCreateResource(resolver, ResourceUtil.getParent(createPath),copyResource.getResourceType(), null, false);
                    return (AdapterType)newResource.adaptTo(ModifiableValueMap.class);
                } catch ( final PersistenceException pe) {
                    // we ignore this for now
                    return null;
                }
            }
            final String resourcePath = paths[paths.length-1];
            final Resource rsrc = resolver.getResource(resourcePath);
            return (AdapterType)rsrc.adaptTo(ModifiableValueMap.class);
        }
        return super.adaptTo(type);
    }


    // ---- Object ------------------------------------------------------------

    /**
     * Merged resources are considered equal if their paths are equal,
     * regardless of the list of mapped resources.
     *
     * @param o Object to compare with
     * @return Returns <code>true</code> if the two merged resources have the
     *         same path.
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o.getClass() != getClass()) {
            return false;
        }

        final Resource r = (Resource) o;
        return r.getPath().equals(getPath());
    }

    @Override
    public int hashCode() {
        return this.getPath().hashCode();
    }

    @Override
    public String toString() {
        return "MergedResource [path=" + this.path +
               ", resources=" + this.metadata.get(MergedResourceConstants.METADATA_RESOURCES) + "]";
    }
}
