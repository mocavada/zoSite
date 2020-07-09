package com.moc.aem.zosite.core.models;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.jcr.Session;
import java.util.*;

@Model(
        // (Almost) always adapt from the SlingHttpServetlRequest object; Adapting from multiple classes is supported,
        // however often results in unsatisfied injections and complex logic in the @PostConstruct to derive the required
        // field values.
        // The resourceType is required if you want Sling to "naturally"
        // expose this model as the exporter for a Resource.
        adaptables = { SlingHttpServletRequest.class},
        resourceType = "zo/components/structure/page",
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json", options = {
        @ExporterOption(name = "MapperFeature.SORT_PROPERTIES_ALPHABETICALLY", value = "true"),
        @ExporterOption(name = "SerializationFeature.WRITE_DATES_AS_TIMESTAMPS", value="false")
})
public class ZoPageExporter {
    @Self
    private SlingHttpServletRequest request;

    @Self
    private Resource resource;

    // Inject a property name whose name does NOT match the Model field name
    // Since the Default inject strategy is OPTIONAL (set on the @Model), we can mark injections as @Required. @Optional can be used if the default strategy is REQUIRED.
    @ValueMapValue
    @Named("jcr:title")
    @Required
    private String title;

    // Inject a fields whose property name DOES match the model field name
    @ValueMapValue
    @Optional
    private String pageTitle;

    // Mark as Optional
    @ValueMapValue
    @Optional
    private String navTitle;

    // Provide a default value if the property name does not exist
    @ValueMapValue
    @Named("jcr:description")
    @Default(values = "No description provided")
    private String description;

    // Various data types can be injected
    @ValueMapValue
    @Named("jcr:created")
    private Calendar createdAt;

    @ValueMapValue
    @Default(booleanValues = false)
    boolean navRoot;

    // Inject OSGi services
    @OSGiService
    @Required
    private QueryBuilder queryBuilder;

    // Injection will occur over all Injectors based on Ranking;
    // Force an Injector using @Source(..)
    // If an Injector is not working; ensure you are using the latest version of Sling Models
    @SlingObject
    @Required
    private ResourceResolver resourceResolver;

    // Internal state populated via @PostConstruct logic
    private Iterator<Page> childPageTitle;
    private Iterator<Page> childPagePath;
    private long size;
    private int countx = 0;
    private String queryString;
    private Object[] resultPage;

    // Internal state populated via @PostConstruct logic
    @ScriptVariable(name="currentPage")
    private Page page;

    @PostConstruct
    // PostConstructs are called after all the injection has occurred, but before the Model object is returned for use.
    private void init() {
        // Note that @PostConstruct code will always be executed on Model instantiation.
        // If the work done in PostConstruct is expensive and not always used in the consumption of the model, it is
        // better to lazy-execute the logic in the getter and persist the result in  model state if it is requested again.

        final Map<String, String> map = new HashMap<String, String>();
        // Injected fields can be used to define logic
        map.put("path", page.getPath());
        map.put("type", "cq:Page");

        Query query = queryBuilder.createQuery(PredicateGroup.create(map), resourceResolver.adaptTo(Session.class));
        final SearchResult result = query.getResult();
        this.queryString = result.getQueryStatement();
        this.size = result.getHits().size();
        this.resultPage =  result.getResultPages().toArray();

        this.childPageTitle = resourceResolver.adaptTo(PageManager.class).getContainingPage(page.getPath()).listChildren();
        this.childPagePath = resourceResolver.adaptTo(PageManager.class).getContainingPage(page.getPath()).listChildren();
    }


    public String getQueryString() {
        return this.queryString;
    }

    public Object[] getResultPage() {
        return this.resultPage;
    }


    public List<String> getChildTitle() {
        List<String> list = new ArrayList<>();
        if (this.childPageTitle == null) {
            list.isEmpty();
        } else {
            while(this.childPageTitle.hasNext()) {
                String title = this.childPageTitle.next().getTitle();
                list.add(title);
            }
        }
        return list;
    }

    public List<String> getChildPath() {
        List<String> items = new ArrayList<>();
        if (this.childPagePath == null) {
            items.isEmpty();
        } else {
            while(this.childPagePath.hasNext()) {
                String path = this.childPagePath.next().getPath();
                items.add(path);
            }
        }
        return items;
    }


    public String getTitle() {
        return StringUtils.defaultIfEmpty(pageTitle, title);
    }

    public String getDescription(int truncateAt) {
        if (this.description.length() > truncateAt) {
            return StringUtils.substring(this.description, truncateAt) + "...";
        } else {
            return this.description;
        }
    }

    public String getPath() {
        return page.getPath();
    }

    public String getDescription() {
        // This is just an example of including business logic in the Sling Model;
        return this.getDescription(100);
    }

    public long getSize() {
        return this.size;
    }

    // @JsonIgnore is a Jackson Annotation specific to this field that prevents this field from being serialized into the exported JSON.
    // For a list of Jackson Annotations see https://github.com/FasterXML/jackson-annotations/wiki/Jackson-Annotations
    @JsonIgnore
    public Calendar getCreatedAt() {
        return createdAt;
    }

    @JsonProperty(value = "goodbye-world")
    public String goodbyeWorld() {

        return "Goodbye World";
    }

    public String getHelloWorld() {
        return "Hello World";
    }


}
