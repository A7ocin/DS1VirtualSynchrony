#include <iostream>
#include <string>
#include <fstream>  

int main(int argc, char *argv[]) {

	std::string address, port, manager, managerPort;
	std::string configPath = "build/resources/main/";

	std::cout << "Manager?[y/n] ";
	std::cin >> manager;
	std::cout << "IP address: ";
	std::cin >> address;
	std::cout << "Port: ";
	std::cin >> port;
	if (manager == "n") {
		std::cout << "Manager port: ";
		std::cin >> managerPort;
	}

	std::ofstream outfile(configPath + "actor" + port + ".conf");
	outfile << "akka{\nloglevel = \"OFF\"\nstdout-loglevel = \"OFF\"\nactor {\nprovider = remote\nwarn-about-java-serializer-usage = false\n}\nremote{\nenabled-transports = [\"akka.remote.netty.tcp\"]\nnetty.tcp{\nhostname = \"" + address + "\"\nport = " + port + "\n}\n}\n}\nnodeapp{\n";
	if (manager == "y") {
		outfile << "id = 0\n}";
	}
	else {
		outfile << "id = " + port + "\nremote_ip = \"" + address + "\"\nremote_port = " + managerPort + "\n}";
	}

	outfile.close();

	std::string cmdStr = "gradle run -Dconfig=actor" + port + ".conf";
	const char* cmd = cmdStr.c_str();
	system(cmd);
	std::cin.ignore();
	std::cin.ignore();

	return 0;
}