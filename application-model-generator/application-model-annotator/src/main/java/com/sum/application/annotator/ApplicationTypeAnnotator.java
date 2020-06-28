/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sum.application.annotator;

import com.sun.codemodel.*;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

public class ApplicationTypeAnnotator extends ApplicationCoreTypeAnnotator {

    @Override
    public void processBuildable(JDefinedClass clazz) {
      try {
        JAnnotationUse buildable = clazz.annotate(Buildable.class)
          .param("editableEnabled", false)
          .param("validationEnabled", false)
          .param("generateBuilderPackage", false)
          .param("lazyCollectionInitEnabled", false)
          .param("builderPackage", "com.sum.application.api.builder");

        buildable.paramArray("inline").annotate(Inline.class)
          .param("type", new JCodeModel()._class("com.sum.application.api.model.Doneable"))
          .param("prefix", "Doneable")
          .param("value", "done");

        JAnnotationArrayMember arrayMember = buildable.paramArray("refs");
        arrayMember.annotate(BuildableReference.class).param("value", new JCodeModel()._class("com.sum.application.api.model.ObjectMeta"));
        arrayMember.annotate(BuildableReference.class).param("value", new JCodeModel()._class("com.sum.application.api.model.LabelSelector"));
        arrayMember.annotate(BuildableReference.class).param("value", new JCodeModel()._class("com.sum.application.api.model.Container"));
        arrayMember.annotate(BuildableReference.class).param("value", new JCodeModel()._class("com.sum.application.api.model.PodTemplateSpec"));
        arrayMember.annotate(BuildableReference.class).param("value", new JCodeModel()._class("com.sum.application.api.model.ResourceRequirements"));
        arrayMember.annotate(BuildableReference.class).param("value", new JCodeModel()._class("com.sum.application.api.model.IntOrString"));
        arrayMember.annotate(BuildableReference.class).param("value", new JCodeModel()._class("com.sum.application.api.model.ObjectReference"));
        arrayMember.annotate(BuildableReference.class).param("value", new JCodeModel()._class("com.sum.application.api.model.LocalObjectReference"));
        arrayMember.annotate(BuildableReference.class).param("value", new JCodeModel()._class("com.sum.application.api.model.PersistentVolumeClaim"));

      } catch (JClassAlreadyExistsException e) {
        e.printStackTrace();
      }
    }

}
