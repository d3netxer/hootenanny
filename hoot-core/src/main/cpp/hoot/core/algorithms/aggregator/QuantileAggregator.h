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
 * @copyright Copyright (C) 2013, 2015 DigitalGlobe (http://www.digitalglobe.com/)
 */
#ifndef QUANTILEAGGREGATOR_H
#define QUANTILEAGGREGATOR_H

#include "ValueAggregator.h"

namespace hoot
{

using namespace std;

class QuantileAggregator : public ValueAggregator
{
public:

  static string className() { return "hoot::QuantileAggregator"; }

  QuantileAggregator();

  /**
   * quantile - A value from 0 to 1 for the quantile.
   */
  QuantileAggregator(double quantile);

  virtual double aggregate(vector<double>& d) const;

  virtual QString toString() const { return QString("QuantileAggregator %1").arg(_quantile); }

private:
  double _quantile;
};

}

#endif // QUANTILEAGGREGATOR_H
