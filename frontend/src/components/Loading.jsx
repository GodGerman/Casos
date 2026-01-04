import React from 'react';

export default function Loading({ text }) {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center gap-3 py-5 text-secondary animate-fade-in">
      <div className="spinner-border text-primary-500" role="status" style={{ width: '2rem', height: '2rem', borderWidth: '3px' }} />
      <span className="text-sm fw-medium tracking-wide text-uppercase">{text || 'Cargando...'}</span>
    </div>
  );
}
