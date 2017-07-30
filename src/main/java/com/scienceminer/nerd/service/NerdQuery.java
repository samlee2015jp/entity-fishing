package com.scienceminer.nerd.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scienceminer.nerd.disambiguation.NerdContext;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.disambiguation.Sentence;
import com.scienceminer.nerd.disambiguation.WeightedTerm;
import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.kb.Category;
import com.scienceminer.nerd.kb.Statement;
import com.scienceminer.nerd.utilities.Filter;
import com.scienceminer.nerd.utilities.NerdRestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.data.Entity;
import org.grobid.core.document.Document;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.Page;
import org.grobid.core.utilities.KeyGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.grobid.core.lang.Language.*;

/**
 * This is the POJO object for representing input and output "enriched" query.
 * Having Jersey supporting JSON/object mapping, this permits to consume JSON post query.
 *
 * @author Patrice
 */
public class NerdQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(NerdQuery.class);

    public static final String QUERY_TYPE_TEXT = "text";
    public static final String QUERY_TYPE_SHORT_TEXT = "shortText";
    public static final String QUERY_TYPE_TERM_VECTOR= "termVector";
    public static final String QUERY_TYPE_INVALID= "invalid";

    // main text component
    private String text = null;

    // alternative text components for patent input
    private String abstract_ = null;
    private String claims = null;
    private String description = null;

    // search query
    private String shortText = null;

    // language of the query
    private Language language = null;

    // the result of the query disambiguation and enrichment for each identified entities
	private List<NerdEntity> entities = null;

    // the sentence position if such segmentation is to be realized
	private List<Sentence> sentences = null;

    // a list of optional language codes for having multilingual Wikipedia sense correspondences
    // note that the source language is by default the language of results, here ae additional
    // result correspondences in target languages for each entities
	private List<String> resultLanguages = null;

    // runtime in ms of the last processing
    private long runtime = 0;

    // mode indicating if we disambiguate or not
    private boolean onlyNER = false;
    private boolean nbest = false;
    private boolean sentence = false;
    private NerdRestUtils.Format format = NerdRestUtils.Format.valueOf("JSON");
    private String customisation = "generic";

    // list of sentences to be processed
    private Integer[] processSentence = null;

    // weighted vector to be disambiguated
	private List<WeightedTerm> termVector = null;

    // distribution of (Wikipedia) categories corresponding to the disambiguated object
    // (text, term vector or search query)
	private List<Category> globalCategories = null;

    // in case the input to be processed is a list of LayoutToken (text then ust be null)
	private List<LayoutToken> tokens = null;

    private NerdContext context = null;

	// only the entities fullfilling the constraints expressed in the filter will be 
	// disambiguated and outputed
	private Filter filter = null;
 
    // indicate if the full description of the entities should be included in the result
    private boolean full = false;

	public NerdQuery() {
	}

    public NerdQuery(NerdQuery query) {
        this.text = query.getText();
        this.shortText = query.getShortText();
        this.tokens = query.getTokens();

        this.abstract_ = query.getAbstract_();
        this.claims = query.getClaims();
        this.description = query.getDescription();

        this.language = query.getLanguage();
        this.entities = query.getEntities();
        this.sentences = query.getSentences();
        this.resultLanguages = query.getResultLanguages();

        this.onlyNER = query.getOnlyNER();
        this.nbest = query.getNbest();
        this.sentence = query.getSentence();
        this.format = query.getFormat();
        this.customisation = query.getCustomisation();
        this.processSentence = query.getProcessSentence();

        this.termVector = query.getTermVector();
        this.globalCategories = query.getGlobalCategories();

        this.filter = filter;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAbstract_() {
        return abstract_;
    }

    public void setAbstract_(String abstract_) {
        this.abstract_ = abstract_;
    }

    public String getClaims() {
        return claims;
    }

    public void setClaims(String claims) {
        this.claims = claims;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLanguage(Language lang) {
        this.language = lang;
    }

    public Language getLanguage() {
        return language;
    }

    public void setResultLanguages(List<String> langs) {
        this.resultLanguages = langs;
    }

    public List<String> getResultLanguages() {
        return resultLanguages;
    }

    public void setRuntime(long tim) {
        runtime = tim;
    }

    public long getRuntime() {
        return runtime;
    }

    public List<WeightedTerm> getTermVector() {
        return termVector;
    }

    public void setTermVector(List<WeightedTerm> termVector) {
        this.termVector = termVector;
    }

    public List<NerdEntity> getEntities() {
        return entities;
    }

    public void setAllEntities(List<Entity> nerEntities) {
		if (nerEntities != null) {
			this.entities = new ArrayList<NerdEntity>();
            for (Entity entity : nerEntities) {
                this.entities.add(new NerdEntity(entity));
            }
        }
	}

    public void setEntities(List<NerdEntity> entities) {
        this.entities = entities;
    }

    public void addNerdEntities(List<NerdEntity> theEntities) {
		if (theEntities != null) {
	 		if (this.entities == null) {
				this.entities = new ArrayList<NerdEntity>();
			}
			for(NerdEntity entity : theEntities) {
				this.entities.add(entity);
			}
		}
    }

    public List<Sentence> getSentences() {
        return sentences;
    }

    public void setSentences(List<Sentence> sentences) {
        this.sentences = sentences;
    }

    public boolean getOnlyNER() {
        return onlyNER;
    }

    public void setOnlyNER(boolean onlyNER) {
        this.onlyNER = onlyNER;
    }

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
    }

    public boolean getNbest() {
        return nbest;
    }

    public void setNbest(boolean nbest) {
        this.nbest = nbest;
    }

    public boolean getSentence() {
        return sentence;
    }

    public void setSentence(boolean sentence) {
        this.sentence = sentence;
    }

    public String getCustomisation() {
        return customisation;
    }

    public void setCustomisation(String customisation) {
        this.customisation = customisation;
    }

    public Integer[] getProcessSentence() {
        return processSentence;
    }

    public void setProcessSentence(Integer[] processSentence) {
        this.processSentence = processSentence;
    }

    public NerdRestUtils.Format getFormat() {
        return format;
    }

    public void setFormat(NerdRestUtils.Format format) {
        this.format = format;
    }

    public void addEntities(List<NerdEntity> newEntities) {
		if (entities == null) {
			entities = new ArrayList<>();
		}
		if (newEntities.size() == 0) {
			return;
		}
		for(NerdEntity entity : newEntities) {
			entities.add(entity);
		}
    }

    public void addEntity(NerdEntity entity) {
		if (entities == null) {
			entities = new ArrayList<NerdEntity>();
		}
        entities.add(entity);
    }

    public List<Category> getGlobalCategories() {
        return globalCategories;
    }

    public void setGlobalCategories(List<Category> globalCategories) {
        this.globalCategories = globalCategories;
    }

    public void addGlobalCategory(Category category) {
		if (globalCategories == null) {
			globalCategories = new ArrayList<Category>();
		}
        globalCategories.add(category);
    }

    public List<LayoutToken> getTokens() {
        return tokens;
    }

    public void setTokens(List<LayoutToken> tokens) {
        this.tokens = tokens;
    }

    public NerdContext getContext() {
        return this.context;
    }

    public void setContext(NerdContext nerdContext) {
        this.context = nerdContext;
    }

    public Filter getFilter() {
        return this.filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public boolean getFull() {
        return this.full;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    /*public String toJSONFullClean() {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");

        // server runtime is always present (even at 0.0)
        buffer.append("\"runtime\": " + runtime);

        // parameters
        buffer.append(", \"onlyNER\": " + onlyNER);
        buffer.append(", \"nbest\": " + nbest);

        // parameters
        if ((processSentence != null) && (processSentence.length > 0)) {
            buffer.append(", \"processSentence\": [");
            for (int i = 0; i < processSentence.length; i++) {
                if (i != 0) {
                    buffer.append(", ");
                }
                buffer.append(processSentence[i].intValue());
            }
            buffer.append("]");
        }

        // surface form
        if (text != null) {
            byte[] encoded = encoder.quoteAsUTF8(text);
            String output = new String(encoded);
            buffer.append(", \"text\": \"" + output + "\"");
            if (CollectionUtils.isNotEmpty(sentences)) {
                buffer.append(",").append(Sentence.listToJSON(sentences));
            }
        }

        if (shortText != null) {
            byte[] encoded = encoder.quoteAsUTF8(shortText);
            String output = new String(encoded);
            buffer.append(", \"shortText\": \"" + output + "\"");
        }

        if (CollectionUtils.isNotEmpty(termVector)) {
            buffer.append(", \"termVector\": [ ");
            boolean begin = true;
            for (WeightedTerm term : termVector) {
                if (!begin)
                    buffer.append(", ");
                else
                    begin = false;
                buffer.append(term.toJson());
            }
            buffer.append(" ]");
        }

        String lang = "en"; // default language
        if (language != null) {
            buffer.append(", \"language\": " + language.toJSON());
            lang = language.getLang();
        }

        // if available, document level distribution of categories
        if (CollectionUtils.isNotEmpty(globalCategories)) {
            buffer.append(", \"global_categories\": [");
            boolean first = true;
            for (com.scienceminer.nerd.kb.Category category : globalCategories) {
                byte[] encoded = encoder.quoteAsUTF8(category.getName());
                String output = new String(encoded);
                if (first) {
                    first = false;
                } else
                    buffer.append(", ");
                buffer.append("{\"weight\" : " + category.getWeight() + ", \"source\" : \"wikipedia-" + lang
                        + "\", \"category\" : \"" + output + "\", ");
                buffer.append("\"page_id\" : " + category.getWikiPageID() + "}");
            }
            buffer.append("]");
        }

        if (CollectionUtils.isNotEmpty(entities)) {
            buffer.append(", \"entities\": [");
            boolean first = true;
            for (NerdEntity entity : entities) {
                if (filter != null) {
                    List<Statement> statements = entity.getStatements();
                    if ( (statements == null) && 
                         ( (filter.getValueMustNotMatch() == null) || (filter.getValueMustMatch() != null) ) )
                        continue;
                    if (statements != null) {
                        if (!filter.valid(statements))
                            continue;
                    }
                }

                if (first)
                    first = false;
                else
                    buffer.append(", ");
                buffer.append(entity.toJsonFull());
            }
            buffer.append("]");
        }

        // possible page information
        // page height and width
        if (doc != null) {
            List<Page> pages = doc.getPages();
            boolean first = true;
            if (pages != null) {
                buffer.append(", \"pages\":[");
                for (Page page : pages) {
                    if (first)
                        first = false;
                    else
                        buffer.append(", ");
                    buffer.append("{\"page_height\":" + page.getHeight());
                    buffer.append(", \"page_width\":" + page.getWidth() + "}");
                }
                buffer.append("]");
            }
        }

        buffer.append("}");

        return buffer.toString();
    }*/

    public String toJSONClean(Document doc) {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");

        // server runtime is always present (even at 0.0)
        buffer.append("\"runtime\": " + runtime);

        // parameters
        buffer.append(", \"onlyNER\": " + onlyNER);
        buffer.append(", \"nbest\": " + nbest);

        // parameters
        if (ArrayUtils.isNotEmpty(processSentence)) {
            buffer.append(", \"processSentence\": [");
            for (int i = 0; i < processSentence.length; i++) {
                if (i != 0) {
                    buffer.append(", ");
                }
                buffer.append(processSentence[i].intValue());
            }
            buffer.append("]");
        }

        // surface form
        if (text != null) {
            byte[] encoded = encoder.quoteAsUTF8(text);
            String output = new String(encoded);
            buffer.append(", \"text\": \"" + output + "\"");
            if (CollectionUtils.isNotEmpty(sentences)) {
                buffer.append(",").append(Sentence.listToJSON(sentences));
            }
        }

        if (shortText != null) {
            byte[] encoded = encoder.quoteAsUTF8(shortText);
            String output = new String(encoded);
            buffer.append(", \"shortText\": \"" + output + "\"");
        }

        if (CollectionUtils.isNotEmpty(termVector)) {
            buffer.append(", \"termVector\": [ ");
            boolean begin = true;
            for (WeightedTerm term : termVector) {
                if (!begin)
                    buffer.append(", ");
                else
                    begin = false;
                buffer.append(term.toJson());
            }
            buffer.append(" ]");
        }

        String lang = "en"; // default language
        if (language != null) {
            buffer.append(", \"language\": " + language.toJSON());
            lang = language.getLang();
        }

        // if available, document level distribution of categories
        if (CollectionUtils.isNotEmpty(globalCategories)) {
            buffer.append(", \"global_categories\": [");
            boolean first = true;
            for (com.scienceminer.nerd.kb.Category category : globalCategories) {
                byte[] encoded = encoder.quoteAsUTF8(category.getName());
                String output = new String(encoded);
                if (first) {
                    first = false;
                } else
                    buffer.append(", ");
                buffer.append("{\"weight\" : " + category.getWeight() + ", \"source\" : \"wikipedia-" + lang
                        + "\", \"category\" : \"" + output + "\", ");
                buffer.append("\"page_id\" : " + category.getWikiPageID() + "}");
            }
            buffer.append("]");
        }

        if (CollectionUtils.isNotEmpty(entities)) {
            buffer.append(", \"entities\": [");
            boolean first = true;
            for (NerdEntity entity : entities) {
                if (filter != null) {
                    List<Statement> statements = entity.getStatements();
                    if ( (statements == null) && 
                         ( (filter.getValueMustNotMatch() == null) || (filter.getValueMustMatch() != null) ) )
                        continue;
                    if (statements != null) {
                        if (!filter.valid(statements))
                            continue;
                    }
                }

                if (first)
                    first = false;
                else
                    buffer.append(", ");
                if (this.full)
                    buffer.append(entity.toJsonFull());
                else   
                    buffer.append(entity.toJsonCompact());
            }
            buffer.append("]");
        }

        // possible page information
        // page height and width
        if (doc != null) {
            List<Page> pages = doc.getPages();
            boolean first = true;
            if (pages != null) {
                buffer.append(", \"pages\":[");
                for (Page page : pages) {
                    if (first)
                        first = false;
                    else
                        buffer.append(", ");
                    buffer.append("{\"page_height\":" + page.getHeight());
                    buffer.append(", \"page_width\":" + page.getWidth() + "}");
                }
                buffer.append("]");
            }
        }

        buffer.append("}");

        return buffer.toString();
    }

    @Override
    public String toString() {
        return "Query [text=" + text + ", shortText=" + shortText + ", terms=" + "]";
    }

    /**
     * Export of standoff annotated text in TEI format
     */
    public String toTEI() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><tei xmlns:ng=\"http://relaxng.org/ns/structure/1.0\" xmlns:exch=\"http://www.epo.org/exchange\" xmlns=\"http://www.tei-c.org/ns/1.0\">");
        buffer.append("<teiHeader>");
        buffer.append(" </teiHeader>");
        String idP = KeyGen.getKey();
        buffer.append("<standoff>");
        int n = 0;
        for (NerdEntity entity : entities) {
            //buffer.append(entity.toTEI(idP, n));
            n++;
        }
        buffer.append("</standoff>");
        if (text != null) {
            buffer.append("<text>");
            buffer.append(text);
            buffer.append("</text>");
        }
        if (shortText != null) {
            buffer.append("<text>");
            buffer.append(shortText);
            buffer.append("</text>");
        }
        buffer.append("</tei>");

        return buffer.toString();
    }

    /**
     * Check that language has been correctly set
     */
    public boolean hasValidLanguage() {
        return (language != null && language.getLang() != null)
                && (language.getLang().equals(EN) || language.getLang().equals(DE) || language.getLang().equals(FR)) ;
    }

    public static NerdQuery fromJson(String theQuery) throws QueryException {
        if(StringUtils.isEmpty(theQuery)) {
            throw new QueryException("The query cannot be null:\n " + theQuery);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            return mapper.readValue(theQuery, NerdQuery.class);
        } catch(JsonGenerationException | JsonMappingException e) {
            throw new QueryException("JSON cannot be processed:\n " + theQuery + "\n ", e);
        } catch(IOException e) {
            throw new QueryException("Some serious error when deserialize the JSON object: \n" + theQuery, e);
        }
    }

    /**
     * if text is valid, shortText is set to null... and vice-versa
     */
    @JsonIgnore
    public String getQueryType() {
        if (isNotBlank(text) && (text.trim().length() > 1)) {
            shortText = null;
            return QUERY_TYPE_TEXT;
        } else if (isNotEmpty(shortText)) {
            text = null;
            return QUERY_TYPE_SHORT_TEXT;
        } else if (CollectionUtils.isNotEmpty(termVector)) {
            return QUERY_TYPE_TERM_VECTOR;
        } else {
            return QUERY_TYPE_INVALID;
        }
    }

}