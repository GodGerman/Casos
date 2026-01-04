import React from "react";
import {Routes, Route,Link } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css';

import Login from "./Login.jsx";
import Administrator from "./Administrator.jsx";
class BootstrapReact extends React.Component {

  render() {
    return(
      <div>
        <div class="container-fluid p-3 custom-header-bg text-white text-center">
          <h1>LOGIN</h1>
        </div>

        <div class="container mt-5">
          <div class="row">
            <div class="col-sm-12">
              {/* Un <Routes> mira a través de sus hijos <Route>s y 
                ejecuta el primero que coincida con el URL actual. */}
              <Routes>
                <Route path="/" element={<Login />} />
                <Route path="/administrator" element={<Administrator />} />                          
              </Routes>
            </div>
          </div>
        </div>
      </div>
    );    
  }
}
export default BootstrapReact; 