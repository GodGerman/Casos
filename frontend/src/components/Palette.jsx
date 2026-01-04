import React from 'react';
import ElementIcon from './ElementIcon.jsx';

const ITEMS = [
  { tipo: 'ACTOR', etiqueta: 'Actor' },
  { tipo: 'CASO_DE_USO', etiqueta: 'Caso de Uso' },
  { tipo: 'LIMITE_SISTEMA', etiqueta: 'Límite' },
  { tipo: 'PAQUETE', etiqueta: 'Paquete' },
  { tipo: 'NOTA', etiqueta: 'Nota' },
  { tipo: 'TEXTO', etiqueta: 'Texto' },
  { tipo: 'IMAGEN', etiqueta: 'Imagen' },
  { tipo: 'RELACION', etiqueta: 'Relación' }
];

export default function Palette() {
  const onDragStart = (event, tipo) => {
    event.dataTransfer.setData('tipo_elemento', tipo);
    event.dataTransfer.effectAllowed = 'copy';
  };

  return (
    <div className="card shadow-sm border-0 bg-dark-800 h-100">
      <div className="card-header bg-transparent border-dark-700 py-3">
        <h6 className="mb-0 fw-bold text-primary-300 text-uppercase tracking-wider icon-link gap-2">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
          Herramientas
        </h6>
      </div>
      <div className="card-body p-2">
        <div className="row g-2">
          {ITEMS.map((item) => (
            <div key={item.tipo} className="col-6 col-xl-12">
              <div
                className="d-flex flex-column flex-xl-row align-items-center gap-2 p-2 rounded border border-dark-700 bg-dark-900 hover-bg-dark-700 transition-colors cursor-grab h-100"
                draggable
                onDragStart={(event) => onDragStart(event, item.tipo)}
                title={`Arrastra ${item.etiqueta} al lienzo`}
              >
                <div className="text-primary-400 p-1 rounded bg-dark-800">
                  <ElementIcon type={item.tipo} size={20} />
                </div>
                <span className="text-secondary text-xs fw-medium text-center text-xl-start flex-grow-1 text-truncate w-100">
                  {item.etiqueta}
                </span>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-4 px-2">
          <div className="d-flex align-items-start gap-2 text-muted text-xs">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="mt-1 flex-shrink-0"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
            <p className="mb-0 opacity-75">Arrastra los elementos al lienzo para comenzar a diseñar.</p>
          </div>
        </div>
      </div>
    </div>
  );
}
