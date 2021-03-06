/*
 * This file is part of Hootenanny.
 *
 * Hootenanny is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * --------------------------------------------------------------------
 *
 * The following copyright notices are generated automatically. If you
 * have a new notice to add, please use the format:
 * " * @copyright Copyright ..."
 * This will properly maintain the copyright information. DigitalGlobe
 * copyrights will be updated automatically.
 *
 * @copyright Copyright (C) 2014, 2015 DigitalGlobe (http://www.digitalglobe.com/)
 */
#include "TranslatedTagCountVisitor.h"

// hoot
#include <hoot/core/io/ScriptToOgrTranslator.h>
#include <hoot/core/io/schema/Feature.h>
#include <hoot/core/io/schema/FeatureDefinition.h>
#include <hoot/core/io/schema/FieldDefinition.h>
#include <hoot/core/util/ElementConverter.h>

namespace hoot
{

TranslatedTagCountVisitor::TranslatedTagCountVisitor(shared_ptr<ScriptTranslator> t) :
  _map(),
  _populatedCount(),
  _defaultCount(),
  _nullCount()
{
  _translator = dynamic_pointer_cast<ScriptToOgrTranslator>(t);
  if (!_translator)
  {
    throw HootException("Error allocating translator, the translation script must support "
                        "converting to OGR.");
  }

  _schema = _translator->getOgrOutputSchema();
}

void TranslatedTagCountVisitor::_countTags(shared_ptr<Feature>& f)
{
  const shared_ptr<const FeatureDefinition>& defn = f->getFeatureDefinition();

  for (size_t i = 0; i < defn->getFieldCount(); i++)
  {
    shared_ptr<const FieldDefinition> fd = defn->getFieldDefinition(i);

    const QVariantMap& vm = f->getValues();

    if (vm.contains(fd->getName()) == false)
    {
      _nullCount++;
    }
    else
    {
      QVariant v = vm[fd->getName()];

      if (v.isNull())
      {
        _nullCount++;
      }
      else if (fd->hasDefaultValue() && fd->getDefaultValue() == v)
      {
        _defaultCount++;
      }
      else
      {
        _populatedCount++;
      }
    }
  }
}

void TranslatedTagCountVisitor::visit(ElementType type, long id)
{
  shared_ptr<const Element> e = _map->getElement(type, id);

  if (e->getTags().getInformationCount() > 0)
  {
    shared_ptr<Geometry> g = ElementConverter(_map->shared_from_this()).convertToGeometry(e);

    Tags t = e->getTags();
    t["error:circular"] = QString::number(e->getCircularError());
    t["hoot:status"] = e->getStatusString();

    // remove all the empty tags.
    for (Tags::const_iterator it = e->getTags().begin(); it != e->getTags().end(); ++it)
    {
      if (t[it.key()] == "")
      {
        t.remove(it.key());
      }
    }

    QString layerName;
    vector<ScriptToOgrTranslator::TranslatedFeature> f = _translator->translateToOgr(t,
      e->getElementType(), g->getGeometryTypeId());

    // only write the feature if it wasn't filtered by the translation script.
    for (size_t i = 0; i < f.size(); i++)
    {
      _countTags(f[i].feature);
    }
  }
}

}
