/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

#include "PaintNodesMapper.h"

// Hoot
#include <hoot/core/util/HootException.h>
#include <hoot/core/io/PbfReader.h>

// Pretty Pipes
#include <pp/Factory.h>
#include <pp/HadoopPipesUtils.h>
#include <pp/Hdfs.h>

namespace hoot
{

PP_FACTORY_REGISTER(pp::Mapper, PaintNodesMapper)

PaintNodesMapper::PaintNodesMapper()
{
  _context = NULL;
}

void PaintNodesMapper::close()
{
  flush();
}

void PaintNodesMapper::flush()
{
  LOG_INFO("_nd.size() " << _nd.size());
  struct Pixel
  {
    short x;
    short y;
  };
  Pixel* p;

  string key;
  key.resize(sizeof(struct Pixel));
  p = (struct Pixel*)(key.data());
  string value;
  value.resize(sizeof(int));
  int* count = (int*)(value.data());

  long flushSum = 0;
  const QHash<long, int>& h = _nd.getHash();
  for (QHash<long, int>::const_iterator it = h.begin(); it != h.end(); ++it)
  {
    p->x = it.key() % _nd.getWidth();
    p->y = it.key() / _nd.getWidth();
    *count = it.value();
    flushSum += *count;
    _context->emit(key, value);
  }
  LOG_INFO("flushSum: " << flushSum);
  _nd.reset();
}

void PaintNodesMapper::_init(HadoopPipes::MapContext& context)
{
  _context = &context;
  shared_ptr<pp::Configuration> c(pp::HadoopPipesUtils::toConfiguration(context.getJobConf()));
  _envelope = Envelope(c->get("hoot.envelope"));
  LOG_INFO("_envelope: " << _envelope.toString());
  _pixelSize = c->getDouble("hoot.pixel.size");
  _width = ceil(_envelope.getWidth() / _pixelSize) + 1;
  _height = ceil(_envelope.getHeight() / _pixelSize) + 1;
  LOG_INFO("w: " << _width << " h: " << _height);
  _nd.reset(_width, _height);
}

void PaintNodesMapper::_map(shared_ptr<OsmMap>& m, HadoopPipes::MapContext& context)
{
  if (_context == NULL)
  {
    _init(context);
  }

  const OsmMap::NodeMap& nm = m->getNodeMap();
  LOG_INFO("Processing map. Node count: " << nm.size());
  for (OsmMap::NodeMap::const_iterator it = nm.constBegin(); it != nm.constEnd(); ++it)
  {
    const shared_ptr<const Node>& n = it.value();

    int px = int((n->getX() - _envelope.getMinX()) / _pixelSize);
    int py = int((n->getY() - _envelope.getMinY()) / _pixelSize);
    px = std::min(_width - 1, std::max(0, px));
    py = std::min(_height - 1, std::max(0, py));
    _nd.setPixel(px, py, _nd.getPixel(px, py) + 1);

    if (_nd.size() > 1000000)
    {
      flush();
    }
  }
}

}

