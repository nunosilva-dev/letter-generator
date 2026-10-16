-- Application-owned example schema; the library has no database dependency.
-- Store each published template version as a new row.
CREATE TABLE letter_template (
    template_key VARCHAR(100) NOT NULL,
    template_version INTEGER NOT NULL,
    template_format VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    PRIMARY KEY (template_key, template_version)
);
-- template_format = 'LETTER_HTML_V1'
-- content = the complete UTF-8 contents of edp-price-update.html
-- Store image bytes separately; the application resolves asset: identifiers
-- and supplies those bytes to the template builder. Per-letter values are
-- passed as a Map and are not persisted inside the reusable template.
