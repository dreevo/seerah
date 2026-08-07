package com.seerah.content.api;

import com.seerah.content.api.LearningPathViews.PathDetail;
import com.seerah.content.api.LearningPathViews.PathSummary;

import java.util.List;
import java.util.Optional;

/** Published read contract for learning paths. */
public interface LearningPathReadPort {

    List<PathSummary> publishedPaths(String locale);

    Optional<PathDetail> pathBySlug(String slug, String locale);
}
