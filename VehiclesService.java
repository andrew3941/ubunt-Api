package com.preving.intranet.gestioncentrosapi.model.services;


import com.preving.intranet.gestioncentrosapi.model.domain.vehicles.Brands;
import com.preving.intranet.gestioncentrosapi.model.domain.vehicles.Vehicles;
import com.preving.intranet.gestioncentrosapi.model.domain.vehicles.VehiclesFilter;
import com.preving.security.domain.UsuarioWithRoles;
import org.springframework.http.ResponseEntity;



import javax.servlet.http.HttpServletResponse;
import java.util.List;


public interface VehiclesService {


    //    Export Vehicles
    ResponseEntity<?> exportVehicle(int workCenterId, VehiclesFilter vehiclesFilter, HttpServletResponse response, UsuarioWithRoles user);

    //    List all vehicles brand types
    List<Brands> getAllBrandTypes();

    //    Get all vehicle list
    List<Vehicles> findAllVehiclesByWorkCenter(int workCenterId);

    List<Vehicles> getFilteredVehicles(int workCenterId, VehiclesFilter vehiclesFilter, UsuarioWithRoles user);
}
