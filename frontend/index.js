import React, { Component } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter} from 'react-router-dom';
import BootstrapReac from './src/componentes/BootstrapReact.jsx'
 
class Aplicacion extends React.Component {
render() {
      return(
    <BrowserRouter>
      <BootstrapReac />
    </BrowserRouter>
      );    
  }
}
 
export default Aplicacion;

const root = createRoot(document.getElementById('raiz'));
root.render(<Aplicacion />);