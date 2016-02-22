/**
 * Copyright (c) 2016, wring.io
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the wring.io nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.wring.agents.github;

import com.jcabi.aspects.Tv;
import com.jcabi.github.Github;
import com.jcabi.github.RtGithub;
import com.jcabi.github.RtPagination;
import com.jcabi.http.Request;
import com.jcabi.http.response.RestResponse;
import io.wring.agents.Agent;
import io.wring.model.Events;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;
import javax.json.JsonObject;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

/**
 * GitHub agent.
 *
 * @author Yegor Bugayenko (yegor@teamed.io)
 * @version $Id: c79829f9e91907f21c716854779af4233e496fa9 $
 * @since 1.0
 */
public final class AgGithub implements Agent {

    /**
     * GitHub access token.
     */
    private final transient String token;

    /**
     * Ctor.
     * @param json JSON config
     */
    public AgGithub(final JsonObject json) {
        this.token = json.getString("token");
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public void push(final Events events) throws IOException {
        final Github github = new RtGithub(this.token);
        final String since = DateFormatUtils.formatUTC(
            DateUtils.addMinutes(new Date(), -Tv.THREE),
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        );
        final Request req = github.entry()
            .uri().path("/notifications").back();
        final Iterable<JsonObject> list = new RtPagination<>(
            req.uri().queryParam("participating", "true")
                .queryParam("since", since)
                .queryParam("all", Boolean.toString(true))
                .back(),
            RtPagination.COPYING
        );
        for (final JsonObject event : list) {
            final String reason = event.getString("reason");
            if (!"mention".equals(reason)) {
                continue;
            }
            final JsonObject subject = event.getJsonObject("subject");
            events.post(
                subject.getString("title"),
                String.format(
                    "[see](%s)",
                    subject.getString("url")
                )
            );
        }
        req.uri()
            .queryParam("last_read_at", since).back()
            .method(Request.PUT)
            .body().set("{}").back()
            .fetch()
            .as(RestResponse.class)
            .assertStatus(HttpURLConnection.HTTP_RESET);
    }

}