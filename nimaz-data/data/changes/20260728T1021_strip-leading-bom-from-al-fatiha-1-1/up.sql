UPDATE ayahs SET text_arabic = replace(text_arabic, char(65279), ''), text_uthmani = replace(text_uthmani, char(65279), '') WHERE id = 1;
