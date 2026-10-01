package io.quarkiverse.googlecloudservices.genai.runtime.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The Gen AI SDK local tokenizer relies on protobuf classes generated with protobuf 3.x that are not compatible with the
 * protobuf 4.x used by the Google Cloud libraries, so they cannot be compiled into a native image.
 * We delete them: the local tokenizer is not usable in native mode, everything else is not impacted.
 */
final class LocalTokenizerSubstitutions {

    @TargetClass(className = "com.google.genai.proto.SentencepieceModel$ModelProto")
    @Delete
    static final class DeleteModelProto {
    }

    @TargetClass(className = "com.google.genai.proto.SentencepieceModel$ModelProto$Builder")
    @Delete
    static final class DeleteModelProtoBuilder {
    }
}
