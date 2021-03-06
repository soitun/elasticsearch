/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.analysis;

import org.elasticsearch.ElasticSearchIllegalArgumentException;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.ngram.XEdgeNGramTokenizer;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.settings.IndexSettings;

import java.io.Reader;

import static org.elasticsearch.index.analysis.NGramTokenizerFactory.parseTokenChars;

/**
 *
 */
public class EdgeNGramTokenizerFactory extends AbstractTokenizerFactory {

    private final int minGram;

    private final int maxGram;

    private final EdgeNGramTokenizer.Side side;

    private final CharMatcher matcher;

    @Inject
    public EdgeNGramTokenizerFactory(Index index, @IndexSettings Settings indexSettings, @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettings, name, settings);
        this.minGram = settings.getAsInt("min_gram", NGramTokenizer.DEFAULT_MIN_NGRAM_SIZE);
        this.maxGram = settings.getAsInt("max_gram", NGramTokenizer.DEFAULT_MAX_NGRAM_SIZE);
        this.side = EdgeNGramTokenizer.Side.getSide(settings.get("side", EdgeNGramTokenizer.DEFAULT_SIDE.getLabel()));
        this.matcher = parseTokenChars(settings.getAsArray("token_chars"));
    }

    @Override
    public Tokenizer create(Reader reader) {
        if (version.onOrAfter(Version.LUCENE_43)) {
            if (side == EdgeNGramTokenizer.Side.BACK) {
                throw new ElasticSearchIllegalArgumentException("side=BACK is not supported anymore. Please fix your analysis chain or use"
                        + " an older compatibility version (<=4.2) but beware that it might cause highlighting bugs.");
            }
            // LUCENE MONITOR: this token filter is a copy from lucene trunk and should go away once we upgrade to lucene 4.4
            if (matcher == null) {
                return new XEdgeNGramTokenizer(version, reader, minGram, maxGram);
            } else {
                return new XEdgeNGramTokenizer(version, reader, minGram, maxGram) {
                    @Override
                    protected boolean isTokenChar(int chr) {
                        return matcher.isTokenChar(chr);
                    }
                };
            }
        } else {
            return new EdgeNGramTokenizer(reader, side, minGram, maxGram);
        }
    }
}