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
#ifndef SCRIPTMATCH_H
#define SCRIPTMATCH_H

// hoot
#include <hoot/core/conflate/Match.h>
#include <hoot/core/conflate/MatchThreshold.h>
#include <hoot/core/conflate/MatchDetails.h>
#include <hoot/js/PluginContext.h>

namespace hoot
{

class ScriptMatchTest;

/**
 * The script match and merge routines expose a way for matching and merging elements using
 * JavaScript. See the _JavaScript Overview_ in the _Supplemental User Documentation_ for details.
 */
class ScriptMatch : public Match, public MatchDetails
{
public:
  ScriptMatch(shared_ptr<PluginContext> script, Persistent<Object> plugin,
              const ConstOsmMapPtr& map, const ElementId& eid1, const ElementId& eid2,
              ConstMatchThresholdPtr mt);

  virtual const MatchClassification& getClassification() const { return _p; }

  virtual QString explain() const { return _explainText; }

  virtual double getProbability() const;

  /**
   *
   */
  virtual bool isConflicting(const Match& other, const ConstOsmMapPtr& map) const;

  virtual bool isWholeGroup() const { return _isWholeGroup; }

  /**
   * Simply returns the two elements that were matched.
   */
  virtual set< pair<ElementId, ElementId> > getMatchPairs() const;

  Persistent<Object> getPlugin() const { return _plugin; }

  shared_ptr<PluginContext> getScript() const { return _script; }

  virtual QString toString() const;

  virtual map<QString, double> getFeatures(const ConstOsmMapPtr& map) const;

private:

  ElementId _eid1, _eid2;
  bool _isWholeGroup;
  MatchClassification _p;
  Persistent<Object> _plugin;
  shared_ptr<PluginContext> _script;
  QString _explainText;
  typedef pair<ElementId, ElementId> ConflictKey;
  mutable QHash<ConflictKey, bool> _conflicts;

  friend class ScriptMatchTest;

  void _calculateClassification(const ConstOsmMapPtr& map, Handle<Object> plugin);
  Handle<Value> _call(const ConstOsmMapPtr& map, Handle<Object> plugin);
  ConflictKey _getConflictKey() const { return ConflictKey(_eid1, _eid2); }
  bool _isOrderedConflicting(const ConstOsmMapPtr& map, ElementId sharedEid,
    ElementId other1, ElementId other2) const;
  Handle<Value> _callGetMatchFeatureDetails(const ConstOsmMapPtr& map) const;

};

}

#endif // JSMATCH_H
