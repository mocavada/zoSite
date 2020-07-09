package com.moc.aem.zosite.core.servlets;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.jcr.*;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component(
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Servlet to Render Product Data",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                "sling.servlet.paths=" + "/bin/zoProduct"
        })
public class ZoProductDataJSON extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    @Reference
    private QueryBuilder queryBuilder;

    /** The logger */
    private static final Logger logger = LoggerFactory.getLogger(ZoProductDataJSON.class);

    @SuppressWarnings("deprecation")
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        final ResourceResolver resolver = request.getResourceResolver();
        final Map<String, String> map = new HashMap<String, String>();

        map.put("path", "/var/commerce/products/we-retail");
        map.put("type", "nt:unstructured");
        map.put("property", "cq:commerceType");
        map.put("property.value", "product");
        map.put("p.offset", "0");
        map.put("p.limit", "-1");
        try {
            JSONObject json = new JSONObject();
            JSONObject jsonObj = null;
            JSONObject jsonVariant = null;

            Query query = queryBuilder.createQuery(PredicateGroup.create(map), resolver.adaptTo(Session.class));
            final SearchResult result = query.getResult();
            String size = String.valueOf(result.getHits().size());
            logger.info(size);
            logger.info(result.getQueryStatement());
            Iterator<Node> nodeItr = result.getNodes();
            String productTitle = new String();
            while (nodeItr.hasNext()) {
                jsonObj = new JSONObject();
                logger.info("inside while");
                Node productNode = nodeItr.next();
                PropertyIterator pitr = productNode.getProperties();
                while (pitr.hasNext()) {
                    Property p = pitr.nextProperty();
                    if (p.getName().equalsIgnoreCase("jcr:title")) {
                        jsonObj.put("jcr:title", p.getString());
                        productTitle = p.getString();
                        logger.info(productTitle);
                    } else if (p.getName().equalsIgnoreCase("summary")) {
                        jsonObj.put("summary", p.getString().replaceAll("\\<.*?>",""));
                        logger.info(p.getString());
                    } else if (p.getName().equalsIgnoreCase("cq:tags")) {
                        int max = (p.getValues()).length;
                        String gender = new String();
                        String temp = new String();
                        for (int i = 0; i < max; i++) {
                            temp = p.getValues()[i].toString();
                            if (temp.contains("gender")) {
                                gender = temp.split("/")[1];
                                break;
                            }

                        }
                        jsonObj.put("gender", gender);
                        logger.info(gender);
                    } else if (p.getName().equalsIgnoreCase("features")) {
                        jsonObj.put("features", (p.getString().replaceAll("\\<.*?>","")));
                        logger.info(p.getString());
                    }else if (p.getName().equalsIgnoreCase("jcr:description")) {
                        jsonObj.put("category", (p.getString()));
                        logger.info(p.getString());
                    }
                    else if (p.getName().equalsIgnoreCase("price")) {
                        jsonObj.put("price", p.getString());
                        logger.info(p.getString());
                    } else if (p.getName().equalsIgnoreCase("identifier")) {
                        jsonObj.put("skuidentifier", p.getString());
                        logger.info(p.getString());
                    } else if (p.getName().equalsIgnoreCase("cq:commerceType")) {
                        jsonObj.put("cq:commerceType", p.getString());
                        logger.info(p.getString());
                    } else if (p.getName().equalsIgnoreCase("cq:productVariantAxes")) {

                        NodeIterator variantItr = productNode.getNodes();
                        jsonVariant = new JSONObject();
                        while (variantItr.hasNext()) {

                            Node variant = variantItr.nextNode();
                            logger.info("variant name "+variant.getName().contains("size")+" "+variant.getName());
                            PropertyIterator propItr = variant.getProperties();
                            if (variant.getName().equals("image")) {
                                while (propItr.hasNext()) {
                                    Property fileReference = propItr.nextProperty();
                                    if ("fileReference".equals(fileReference.getName())) {
                                        jsonObj.put("image", fileReference.getString());
                                    }

                                }
                            } else if (variant.getName().contains("size")) {
                                while (propItr.hasNext()) {
                                    Property variantSize = propItr.nextProperty();
                                    if ("size".equals(variantSize.getName())) {
                                        jsonVariant.put(variant.getName(), variantSize.getString());
                                    }
                                }

                            }else{
                                while (propItr.hasNext()) {
                                    Property variantprop = propItr.nextProperty();
                                    if (variantprop.getName().equals("color")) {
                                        JSONObject colorObj = new JSONObject();
                                        logger.info(variantprop.getString());
                                        NodeIterator sizes = variant.getNodes();
                                        while (sizes.hasNext()) {
                                            Node coloredSize = sizes.nextNode();
                                            if(coloredSize.getName().contains("size")){
                                                PropertyIterator propColorItr = coloredSize.getProperties();
                                                while (propColorItr.hasNext()) {
                                                    Property variantColoredSize = propColorItr.nextProperty();
                                                    if (variantColoredSize.getName().equals("size")) {
                                                        colorObj.put(coloredSize.getName(), variantColoredSize.getString());
                                                        logger.info(variantColoredSize.getString());
                                                    }
                                                }
                                            }
                                        }
                                        jsonVariant.put(variantprop.getString(), colorObj);
                                    }

                                }}

                        }
                        jsonObj.put(p.getValues()[0].getString(), jsonVariant);

                    }

                }
                json.put(productTitle, jsonObj);

            }
            response.getWriter().print(json.toString());
            response.setStatus(SlingHttpServletResponse.SC_OK);
        } catch (RepositoryException rpe) {
            rpe.printStackTrace();
        } catch (JSONException e) {

            e.printStackTrace();

        }

    }

}
