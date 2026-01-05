import React, { useMemo } from 'react';
import UMLElement from './canvas/UMLElement.jsx';

// Estilos de linea y marcador segun tipo de conexion UML.
// Se usan para configurar dash y markerEnd en el SVG.
const CONNECTION_STYLE = {
  ASOCIACION: { dash: null, marker: null },
  INCLUSION: { dash: '6 4', marker: 'arrow' },
  EXTENSION: { dash: '6 4', marker: 'arrow' },
  GENERALIZACION: { dash: null, marker: 'triangle' },
  DEPENDENCIA: { dash: '4 4', marker: 'arrow' },
  ENLACE_NOTA: { dash: '2 3', marker: 'arrow' }
};

/**
 * Convierte un tipo en clase CSS normalizada.
 *
 * Se convierte a minusculas y se reemplazan underscores para
 * mantener convenciones de CSS.
 *
 *
 * @param {string} tipo tipo de elemento.
 * @returns {string} clase CSS o cadena vacia.
 */
const toTypeClass = (tipo) => {
  if (!tipo) {
    return '';
  }
  return `tipo-${tipo.toLowerCase().replace(/_/g, '-')}`;
};

/**
 * Lienzo del editor: renderiza elementos y conexiones, y recibe drag & drop.
 *
 * Se calcula un mapa id->elemento para resolver conexiones,
 * dibuja lineas SVG con markers y etiqueta en el punto medio, y posiciona
 * cada elemento como div absoluto para permitir drag y seleccion.
 *
 *
 * @param {object} props props del canvas.
 * @param {React.RefObject} props.canvasRef referencia al contenedor para medir offset.
 * @param {number} props.width ancho del lienzo.
 * @param {number} props.height alto del lienzo.
 * @param {Array} props.elements elementos a renderizar.
 * @param {Array} props.connections conexiones a renderizar.
 * @param {number|null} props.selectedElementId id del elemento seleccionado.
 * @param {Function} props.onDrop handler de drop para crear elementos.
 * @param {Function} props.onDragOver handler para permitir drop.
 * @param {Function} props.onElementMouseDown handler para drag de elementos.
 * @param {Function} props.onElementClick handler para seleccionar elementos.
 * @returns {JSX.Element} contenedor con SVG de conexiones y elementos UML.
 */
export default function Canvas({
  canvasRef,
  width,
  height,
  elements,
  connections,
  selectedElementId,
  onDrop,
  onDragOver,
  onElementMouseDown,
  onElementClick
}) {
  const elementMap = useMemo(() => {
    // Acceso rapido por id para construir conexiones.
    const map = new Map();
    elements.forEach((el) => map.set(el.id_elemento, el));
    return map;
  }, [elements]);

  const lines = useMemo(() => {
    // Calcula coordenadas centro-centro y estilos para dibujar lineas.
    // Se omiten conexiones cuyos elementos no existan en memoria.
    return connections
      .map((conn) => {
        const from = elementMap.get(conn.id_elemento_origen);
        const to = elementMap.get(conn.id_elemento_destino);
        if (!from || !to) {
          return null;
        }
        const style = CONNECTION_STYLE[conn.tipo_conexion] || CONNECTION_STYLE.ASOCIACION;
        // Punto medio para posicionar la etiqueta sin medir texto.
        const midX = (from.pos_x + from.ancho / 2 + to.pos_x + to.ancho / 2) / 2;
        const midY = (from.pos_y + from.alto / 2 + to.pos_y + to.alto / 2) / 2;
        return {
          id: conn.id_conexion,
          x1: from.pos_x + from.ancho / 2,
          y1: from.pos_y + from.alto / 2,
          x2: to.pos_x + to.ancho / 2,
          y2: to.pos_y + to.alto / 2,
          tipo: conn.tipo_conexion,
          etiqueta: conn.etiqueta,
          dash: style.dash,
          marker: style.marker,
          midX,
          midY
        };
      })
      .filter(Boolean);
  }, [connections, elementMap]);

  return (
    <div className="canvas card border-dark-700 shadow-sm overflow-hidden h-100">
      <div className="card-header bg-dark-800 border-dark-700 d-flex justify-content-between align-items-center py-2 px-3">
        <span className="text-secondary text-sm font-monospace">
          CANVAS <span className="text-primary-500">{width}x{height}</span>
        </span>
        <div className="d-flex gap-2">
          <span className="badge bg-dark-700 text-secondary border border-dark-600">Zoom: 100%</span>
        </div>
      </div>
      <div
        className="canvas-body position-relative flex-grow-1"
        ref={canvasRef}
        onDrop={onDrop}
        onDragOver={onDragOver}
        style={{
          width,
          height,
          backgroundColor: 'var(--bg-dark-900)',
          backgroundImage: 'radial-gradient(var(--bg-dark-700) 1px, transparent 1px)',
          backgroundSize: '20px 20px'
        }}
      >
        <svg className="canvas-lines position-absolute top-0 start-0 pointer-events-none" width={width} height={height} style={{ zIndex: 1 }}>
          <defs>
            {/* Marcadores reutilizables para flechas/triangulos */}
            <marker
              id="arrow"
              viewBox="0 0 10 10"
              refX="9"
              refY="5"
              markerWidth="6"
              markerHeight="6"
              orient="auto-start-reverse"
            >
              <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--text-secondary)" />
            </marker>
            <marker
              id="triangle"
              viewBox="0 0 10 10"
              refX="9"
              refY="5"
              markerWidth="7"
              markerHeight="7"
              orient="auto-start-reverse"
            >
              <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--bg-dark-900)" stroke="var(--text-secondary)" strokeWidth="1.4" />
            </marker>
          </defs>
          {lines.map((line) => (
            <line
              key={line.id}
              x1={line.x1}
              y1={line.y1}
              x2={line.x2}
              y2={line.y2}
              stroke="var(--text-secondary)"
              strokeWidth="2"
              strokeDasharray={line.dash || undefined}
              markerEnd={line.marker ? `url(#${line.marker})` : undefined}
            />
          ))}
          {lines.map((line) => (
            line.etiqueta ? (
              // Etiqueta de conexion posicionada en el punto medio.
              <text key={`${line.id}-label`} x={line.midX} y={line.midY - 8} textAnchor="middle" fill="var(--text-primary)" fontSize="11" className="bg-dark-900 px-1 rounded">
                {line.etiqueta}
              </text>
            ) : null
          ))}
        </svg>

        {elements.map((el) => {
          const isSelected = selectedElementId === el.id_elemento;
          return (
            <div
              key={el.id_elemento}
              className={`diagram-element-wrapper ${selectedElementId === el.id_elemento ? 'selected' : ''}`}
              style={{
                position: 'absolute',
                left: el.pos_x,
                top: el.pos_y,
                width: el.ancho,
                height: el.alto,
                transform: el.rotacion_grados ? `rotate(${el.rotacion_grados}deg)` : undefined,
                zIndex: el.orden_z || 10,
                cursor: 'grab'
              }}
              // Inicia drag del elemento y selecciona al hacer click.
              onMouseDown={(event) => onElementMouseDown(event, el)}
              onClick={(event) => onElementClick(event, el)}
            >
              <div style={{ pointerEvents: 'none', width: '100%', height: '100%' }}>
                <UMLElement
                  element={el}
                  isSelected={isSelected}
                  width={el.ancho}
                  height={el.alto}
                />
              </div>

              {/* Etiqueta fuera del SVG para permitir wrap en algunos tipos */}
              <div
                className="position-absolute w-100 text-center pointer-events-none"
                style={{
                  top: el.tipo_elemento === 'ACTOR' ? '100%' : '50%',
                  left: 0,
                  transform: el.tipo_elemento === 'ACTOR' ? 'translateY(5px)' : 'translateY(-50%)',
                  padding: '0 4px',
                  lineHeight: '1.2'
                }}
              >
                <span className={`text-xs ${el.tipo_elemento === 'NOTA' ? 'text-start d-block p-2' : ''} ${el.tipo_elemento === 'ACTOR' ? '' : 'text-truncate d-block'}`}
                  style={{ color: 'var(--text-primary)', whiteSpace: el.tipo_elemento === 'NOTA' ? 'pre-wrap' : 'nowrap' }}>
                  {el.etiqueta || (el.tipo_elemento === 'TEXTO' ? 'Texto' : '')}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div >
  );
}
